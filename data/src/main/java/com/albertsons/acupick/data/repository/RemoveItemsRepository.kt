package com.albertsons.acupick.data.repository

import android.annotation.SuppressLint
import androidx.security.crypto.EncryptedSharedPreferences
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDtoJsonAdapter
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.domain.utils.timer
import com.albertsons.acupick.infrastructure.coroutine.AlbApplicationCoroutineScope
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

interface RemoveItemsRepository {
    fun monitorForRequests()
    suspend fun saveRemoveItemRequest(removeItemRequestList: List<RemoveItemsRequestDto>)
}

internal class RemoveItemRepositoryImplementation(
    moshi: Moshi,
    private val encryptedSharedPreferences: EncryptedSharedPreferences,
    private val userRepository: UserRepository,
    private val apsRepository: ApsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val acuPickLogger: AcuPickLoggerInterface,
    private val dispatcherProvider: DispatcherProvider,
    private val albApplicationCoroutineScope: AlbApplicationCoroutineScope
) : RemoveItemsRepository {
    private val removeItemDataAdapter = RemoveItemsRequestDtoJsonAdapter(moshi)
    private val newDataEvent = MutableStateFlow<Unit?>(null)
    private var removeItemJob: Job? = null
    private val retryMap: HashMap<String, Long> = HashMap()
    private val mutex: Mutex = Mutex()

    override fun monitorForRequests() {
        // If job is active, exit
        if (removeItemJob?.isActive == true) return

        // Otherwise spawn new work
        removeItemJob = albApplicationCoroutineScope.launch(dispatcherProvider.IO) {
            // Reload queue on timer ticks, login status changes, network changes, and direct requests
            combine(timer(RETRY_DELAY_MILLIS), userRepository.isLoggedIn, networkAvailabilityManager.isConnected, newDataEvent) { _, loggedIn, connected, _ ->
                // When online and logged in process queue
                if (connected && loggedIn) loadQueue() else emptyList()
            }.collect { queue ->
                // Avoid re-entrant queue processing.
                if (!mutex.tryLock(this@RemoveItemRepositoryImplementation)) return@collect

                queue.forEach {

                    // Remove entries that don't parse correctly
                    if (it.dto == null) {
                        removeEntry(it.key)
                        return@forEach
                    }

                    //  Attempt API call and remove on success
                    when (sendRemovedItems(it.dto)) {
                        is ApiResult.Success -> removeEntry(it.key)
                        is ApiResult.Failure.Server -> {
                            // Fetch remaining retries from memory
                            val retries = retryMap.getOrDefault(it.key, MAX_RETRIES)

                            // If retry threshold met, drop entry from queue and retry map
                            if (retries <= 0L) {
                                removeEntry(it.key)
                                retryMap.remove(it.key)
                            } else {
                                // Otherwise, decrement remaining retries in retry map
                                retryMap[it.key] = retries - 1
                            }
                        }
                        else -> {} // Retry all other errors (network , etc.) forever
                    }
                }

                //  Free up mutex so we can process another batch from queue.
                mutex.unlock(this@RemoveItemRepositoryImplementation)
            }
        }
    }

    // /////////
    // API Calls
    // /////////

    private suspend fun sendRemovedItems(requestDto: RemoveItemsRequestDto) =
        apsRepository.recordRemoveItems(requestDto).also {
            if (it is ApiResult.Failure.Server) acuPickLogger.setUserData(SERVER_ERROR_CODE, it.error)
        }

    // /////////
    // Queue accessors
    // /////////

    override suspend fun saveRemoveItemRequest(removeItemRequestList: List<RemoveItemsRequestDto>) {
        //  Add list of requests to queue
        with(encryptedSharedPreferences.edit()) {
            removeItemRequestList.forEach { request ->
                putString(REMOVE_ITEM_PREFS + request.timestamp, removeItemDataAdapter.toJson(request))
            }
            commit()
        }

        //  Notify monitor that data is ready
        newDataEvent.emit(Unit)
    }

    // Load queue entries
    private fun loadQueue(): List<RemoveItemsQueueEntry> =
        encryptedSharedPreferences.all.toList().map {
            RemoveItemsQueueEntry(
                key = it.first,
                dto = try { removeItemDataAdapter.fromJson(it.second as String? ?: "") } catch (e: Exception) { null }
            )
        }

    @SuppressLint("ApplySharedPref")
    private fun removeEntry(key: String) {
        encryptedSharedPreferences.edit().remove(key).commit()
        newDataEvent.value = null
    }

    // /////////
    // Helpers
    // /////////

    private data class RemoveItemsQueueEntry(
        val key: String,
        val dto: RemoveItemsRequestDto?,
    )

    companion object {
        const val REMOVE_ITEM_PREFS = "removeItemPrefs"
        const val SERVER_ERROR_CODE = "Server Error Code"
        const val MAX_RETRIES = 5L
        const val RETRY_DELAY_MILLIS = 15000L
    }
}
