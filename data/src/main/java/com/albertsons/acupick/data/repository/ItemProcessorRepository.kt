package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.MissingItemLocationRequestDto
import com.albertsons.acupick.data.model.request.OfflineMissingItemLocation
import com.albertsons.acupick.data.model.request.SyncOfflineMissingItemsLocationReqDto
import com.albertsons.acupick.data.model.request.wrapActionTime
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ItemProcessorService
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.infrastructure.coroutine.AlbApplicationCoroutineScope
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

interface ItemProcessorRepository : Repository {
    val pickList: StateFlow<ActivityDto?>
    suspend fun captureMissingItemLocation(missingItemLocationRequestDto: MissingItemLocationRequestDto): ApiResult<Unit>
    suspend fun syncOfflineMissingItemsOperations(syncOfflineMissingItemLocationReqDto: SyncOfflineMissingItemsLocationReqDto): ApiResult<Unit>

    suspend fun clearItemProcessorData()
}

class ItemProcessorRepositoryImpl(
    private val itemProcessorService: ItemProcessorService,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
    private val offlineMissingLocFile: File,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val dispatcherProvider: DispatcherProvider,
    private val albApplicationCoroutineScope: AlbApplicationCoroutineScope,
    private val moshi: Moshi,
) : ItemProcessorRepository {
    private val _picklist: MutableStateFlow<ActivityDto?> = MutableStateFlow(null)
    override val pickList: StateFlow<ActivityDto?>
        get() = _picklist

    private var offlineMissingItemLocation: OfflineMissingItemLocation? = null
    // private val missingItemLocationList: MutableList<MissingItemLocationRequestDto> = mutableListOf()

    /**
     * Use to lock critical code blocks that should never be accessed simultaneously (like reading from/writing to disk).
     * See https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#mutual-exclusion
     */
    private val offlineDataMutex = Mutex()

    private var isSyncing = false
    private var syncStartTimestamp: Instant? = null

    init {
        albApplicationCoroutineScope.launch(dispatcherProvider.IO) {
            readMissingItemLocationData()
            // Listen to new "connected" network availability events
            networkAvailabilityManager.isConnected.filter { it }.collect {
                attemptSyncIfAble()
            }
        }
    }

    private suspend fun attemptSyncIfAble() {
        val connected = networkAvailabilityManager.isConnected.first()
        if (shouldAttemptSync(connected)) {
            isSyncing = true
            syncStartTimestamp = Instant.now()
            offlineMissingItemLocation?.let { syncOfflineMissingItemsOperations(it.toSyncOfflineItemRequestDto()) }
        }
    }

    private fun shouldAttemptSync(connected: Boolean): Boolean {
        val syncDataPresent = isSyncDataPresent()
        Timber.v("[shouldAttemptSync] connected=$connected, syncDataPresent=$syncDataPresent")
        return connected && isSyncDataPresent()
    }

    private suspend fun isOnline(): Boolean = networkAvailabilityManager.isConnected.first()
    private suspend fun isOnlineAndNotSyncing(): Boolean = isOnline() && !isSyncing

    private fun isSyncDataPresent(): Boolean = offlineMissingItemLocation?.isSyncDataPresent() ?: false

    override suspend fun captureMissingItemLocation(missingItemLocationRequestDto: MissingItemLocationRequestDto): ApiResult<Unit> {
        return wrapExceptions("addMissingItemLocation") {
            val wrappedRequest = missingItemLocationRequestDto.wrapActionTime()
            val missingItemLocationList: MutableList<ActionTimeWrapper<MissingItemLocationRequestDto>> = mutableListOf()
            if (isOnlineAndNotSyncing()) {
                itemProcessorService.captureMissingItemLocation(missingItemLocationRequestDto).toResult()
            } else {
                missingItemLocationList.addAll(offlineMissingItemLocation?.missingItemLocationsDto.orEmpty())
                Timber.v("[addMissingItemLocation] offline  - networkStateSyncInstant=$networkAvailabilityManager.isConnected.first()")
                if (missingItemLocationList.isNotEmpty()) {
                    missingItemLocationList.find { it.wrapped.itemActivityId == missingItemLocationRequestDto.itemActivityId }.let {
                        missingItemLocationList.remove(it)
                    }
                } else {
                    offlineMissingItemLocation = OfflineMissingItemLocation()
                }
                missingItemLocationList.add(wrappedRequest)
                saveMissingItemLocationData(offlineMissingItemLocation?.copy(actId = pickList.value?.actId, missingItemLocationsDto = missingItemLocationList))
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun syncOfflineMissingItemsOperations(syncOfflineMissingItemLocationReqDto: SyncOfflineMissingItemsLocationReqDto): ApiResult<Unit> {
        return wrapExceptions("syncOfflineMissingOperations") {
            itemProcessorService.syncOfflineMissingItemLocation(syncOfflineMissingItemLocationReqDto).toResult()
        }.also {
            when (it) {
                is ApiResult.Success -> {
                    saveMissingItemLocationData(offlineMissingItemLocation?.copyWithUnsyncedOfflineActionsCleared(ZonedDateTime.ofInstant(syncStartTimestamp, ZoneId.systemDefault())))
                    // clearItemProcessorData()
                    attemptSyncIfAble()
                }

                is ApiResult.Failure.NetworkFailure -> {
                    Timber.d("[syncOfflineMissingOperations] network failure on sync - attempting sync again shortly")
                    delay(SYNC_NETWORK_TIMEOUT_FAILURE_NEXT_SYNC_DELAY_DURATION.toMillis())
                    attemptSyncIfAble()
                }

                is ApiResult.Failure -> {
                    Timber.w("[syncOfflineMissingItemsOperations] unrecoverable error with sync! Removing offline item processor data")
                    saveMissingItemLocationData(offlineMissingItemLocation?.copyWithAllOfflineActionsCleared())
                }
            }
            isSyncing = false
            syncStartTimestamp = null
        }
    }

    override suspend fun clearItemProcessorData() {
        offlineMissingItemLocation = null
        offlineMissingLocFile.delete()
    }

    private suspend fun saveMissingItemLocationData(offlineMissingItemLocationArg: OfflineMissingItemLocation?) {
        withContext(dispatcherProvider.IO) {
            offlineDataMutex.withLock {
                offlineMissingItemLocation = offlineMissingItemLocationArg
                val adapter: JsonAdapter<OfflineMissingItemLocation> = moshi.adapter(OfflineMissingItemLocation::class.java)
                offlineMissingLocFile.sink().buffer().use { sink -> adapter.toJson(sink, offlineMissingItemLocation) }
            }
        }
    }

    private suspend fun readMissingItemLocationData() {
        withContext(dispatcherProvider.IO) {
            offlineDataMutex.withLock {
                if (offlineMissingLocFile.exists()) {
                    try {
                        val adapter: JsonAdapter<OfflineMissingItemLocation> = moshi.adapter(OfflineMissingItemLocation::class.java)
                        offlineMissingItemLocation = adapter.fromJson(offlineMissingLocFile.source().buffer())
                        // missingItemLocationList.clear()
                        // missingItemLocationList.addAll(offlineMissingItemLocationReqDTO?.missingItemLocationList.orEmpty())
                        Timber.v("[readFromFile] offlinePickData=$offlineMissingItemLocation")
                    } catch (e: IOException) {
                        Timber.w(e, "[readFromFile] error reading offline file")
                    }
                }
            }
        }
    }

    /** Delegates to [wrapExceptions], passing in the class name here instead of requiring it of all callers */
    private suspend fun <T : Any> wrapExceptions(methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        return wrapExceptions("ItemProcessorRepository", methodName, block)
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    private val SYNC_NETWORK_TIMEOUT_FAILURE_NEXT_SYNC_DELAY_DURATION: Duration = Duration.ofSeconds(5)
}
