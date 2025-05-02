package com.albertsons.acupick.data.repository

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ItemSearchResult
import com.albertsons.acupick.data.model.LastSubOrOosTime
import com.albertsons.acupick.data.model.LastSubOrOosTimeJsonAdapter
import com.albertsons.acupick.data.model.OfflinePickData
import com.albertsons.acupick.data.model.OnlineInMemoryPickData
import com.albertsons.acupick.data.model.PickRequest
import com.albertsons.acupick.data.model.RequestResponse
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.ShortReasonCode
import com.albertsons.acupick.data.model.SubstitutePickRequest
import com.albertsons.acupick.data.model.alsoOnFailure
import com.albertsons.acupick.data.model.alsoOnSuccess
import com.albertsons.acupick.data.model.asEmptyResult
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.getItemActivityDto
import com.albertsons.acupick.data.model.isAdvancePickOrPrePick
import com.albertsons.acupick.data.model.isWeightedItem
import com.albertsons.acupick.data.model.picklistprocessor.PickListProcessor
import com.albertsons.acupick.data.model.picklistprocessor.PickListProcessorInput
import com.albertsons.acupick.data.model.request.ActionTimeWrapper
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.ChatErrorData
import com.albertsons.acupick.data.model.request.ItemPickCompleteDto
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.LineRequestDto
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.request.PickCompleteRequestDto
import com.albertsons.acupick.data.model.request.ReassignDropOffRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.request.SyncOfflinePickingRequestDto
import com.albertsons.acupick.data.model.request.UndoPickLocalDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.request.combineIdenticalUndoPickLocalDtos
import com.albertsons.acupick.data.model.request.wrapActionTime
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ContainerActivityDto
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.data.model.response.ItemUpcDto
import com.albertsons.acupick.data.model.response.StagingSummaryDto
import com.albertsons.acupick.data.model.response.SubstitutionItemDetailsDto
import com.albertsons.acupick.data.model.response.getListOfConversationSid
import com.albertsons.acupick.data.model.response.getListOfOrderNumber
import com.albertsons.acupick.data.model.response.getListOfStartTimerOrderNumber
import com.albertsons.acupick.data.model.response.isStartTimerEnabled
import com.albertsons.acupick.data.model.response.isSubstitutionOrIssueScanning
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ApsService
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.picklist.PickListOperations
import com.albertsons.acupick.data.toast.Toaster
import com.albertsons.acupick.infrastructure.coroutine.AlbApplicationCoroutineScope
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.hadilq.liveevent.LiveEvent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import kotlin.math.abs
import kotlin.system.measureTimeMillis

/** Use for pick flow related apis and functionality */
interface PickRepository : Repository {

    /**
     * Represents the display pick list which is a combination of:
     * * last activity details source of truth pick list state +
     * * successful online picking activity (not yet reflected in last activity details source of truth) +
     * * offline picking activity (not yet synced therefore not present in the last activity details source of truth)
     */
    val pickList: StateFlow<ActivityDto?>

    /**
     * Master-order-view: Represents master view picklist which is a combination of
     * loggedIn picker items and other picker's item of the same customer order.
     */
    val masterPickList: StateFlow<ActivityDto?>

    /**
     * Master-order-view: Represents newly created iaId in swapSubstitution for other picker's item.
     * BE will clone oldIaId and created new iaId which will be used in undoPick, next recordPick, recordItemPickComplete API's
     */
    val iaIdAfterSwapSubstitution: StateFlow<Long?>

    /**
     * offline sync error that can be watched from anywhere
     */

    val syncError: LiveEvent<Unit>

    val countDownTimer: StateFlow<Int>

    /** True if [pickList.first] is not null, meaning there is an active pick list with offline data */
    suspend fun hasOfflinePickListData(): Boolean

    suspend fun modifyPickList(item: ItemActivityDto)

    /**
     * Set a known pick list id that the current user has assigned to them, even if the full pick list details are not available via [pickList] yet (due to data not being downloaded yet)
     *
     * Example: Picker A signs in a TC-51 where they are already assigned to an order (at which point this function is called to store the activity id)
     */
    fun setActivePickListActivityId(activityId: String)

    /**
     * Get a known pick list id that the current user has assigned to them, even if the full pick list details are not available via [pickList] yet (due to data not being downloaded yet)
     *
     * Example: Picker A signs in a TC-51 where they are already assigned to an order (at which point [setActivePickListActivityId] is called to store the activity id)
     */
    fun getActivePickListActivityId(): String?

    /**
     * True if the picker has a known active pick list. The pick list might/might not have been cached for offline use at this point.
     *
     * Example: Picker A signs in a TC-51 where they are already assigned to an order. Device may or may not have the pick list details cached for offline use.
     */
    fun hasActivePickListActivityId(): Boolean

    suspend fun assignUser(assignUserRequestDto: AssignUserRequestDto): ApiResult<ActivityDto>
    suspend fun unAssignUser(actId: String, userId: String?, tokenizedLdap: String, orderIds: List<String>?): ApiResult<Unit>
    suspend fun reAssignUserStaging(user: ReassignDropOffRequestDto): ApiResult<Unit>
    suspend fun getActivityDetails(id: String, shouldUpdatePickListState: Boolean = true): ApiResult<ActivityDto>
    suspend fun getItemDetails(
        siteId: String,
        upcId: String,
        pluCode: String? = null,
        actId: Long? = null,
        originalItemId: String? = null,
        sellByWeightInd: String? = null,
        queryType: String? = null,
    ): ApiResult<ItemDetailDto>

    suspend fun getSubstitutionItemDetailList(actId: String): ApiResult<List<SubstitutionItemDetailsDto>>
    suspend fun getAllItemLocations(siteId: String, itemId: List<String>): ApiResult<Map<String, List<ItemLocationDto>>>

    /**
     * Retrieves list of [ItemUpcDto] from backend and appends the [ItemActivityDto.primaryUpc] (if not present) to the [ItemUpcDto.upcList] when valid (not null or blank).
     *
     * **Do not assume the last value in the [ItemUpcDto.upcList] is the primary UPC!**
     */
    suspend fun getItemUpcList(siteId: String, itemIds: List<String>, itemActivities: List<ItemActivityDto>): ApiResult<List<ItemUpcDto>>
    suspend fun recordPick(request: PickRequest): ApiResult<Unit>
    suspend fun recordSubstitution(request: SubstitutePickRequest): ApiResult<Unit>
    suspend fun completeClickedCall(itemId: String): ApiResult<Unit>
    suspend fun recordShortage(shortPickRequestDto: ShortPickRequestDto): ApiResult<Unit>
    suspend fun undoPicks(undoPickRequestDtoList: List<UndoPickLocalDto>): ApiResult<Unit>
    suspend fun undoShortage(undoShortRequestDto: UndoShortRequestDto): ApiResult<Unit>
    suspend fun undoShortages(undoShortRequestDtoList: List<UndoShortRequestDto>): Map<UndoShortRequestDto, ApiResult<Unit>>
    suspend fun completePickForStaging(pickReq: PickCompleteRequestDto, tokenizedLdap: String): ApiResult<ActivityDto>
    suspend fun getStagingSummary(actId: String): ApiResult<StagingSummaryDto>
    suspend fun recordItemPickComplete(itemPickCompleteDto: ItemPickCompleteDto): ApiResult<Unit>

    /** Returns the itemId (bpn) associated with a UPC code, or null if no such item is found */
    suspend fun getItemId(itemBarcodeType: BarcodeType.Item?): String?

    /** Returns the suggested substitution item given the item activity ID of the original item */
    fun getSubstitutionItemDetails(iaId: Long): ItemActivityDto?

    /** Returns the suggested substitution item given the item activity ID of the original item */
    fun getAlternateLocations(itemId: String): List<ItemLocationDto>?

    /**
     * Returns the item matching the given [itemBarcodeType] with all order/customer specific info nulled out, or null when there is no match for the given [itemBarcodeType]
     * Useful for scenarios where information on the item is needed but not any additional information for a particular customer order.
     */
    suspend fun getItemWithoutOrderOrCustomerDetails(itemBarcodeType: BarcodeType.Item?): ItemSearchResult

    /**
     * For non-batch picklists, the item matching the given [itemBarcodeType], or null when there is no match for the given [itemBarcodeType].
     * For batch picklists where identical items from different customer orders are present, the matching item is returned following the below rules (with null for no match):
     *
     * * Use the selected item if the barcode lookup item matches (Note: Since the barcode item that was found would be the first occurrence in the picklist, regardless of customer order number).
     * This functionality allows a picker to scroll to a given item and have scans affect the selected item.
     *
     * * Select the first matching item that doesn't allow substitutions so that those customers will have the in stock items picked first and other customer orders that allow substitutions could
     * receive them if stock is not available for all customers item qty for the picklist.
     * Intended to be used when entering a barcode in the main picking flow to select/highlight/scroll to the item returned from this function.
     */
    suspend fun getNextItemToSelectForScan(itemBarcodeType: BarcodeType.Item?, currentSelectedItem: ItemActivityDto?): ItemSearchResult

    /** Returns the item matching the given [itemBarcodeType] and [customerOrderNumber] or null. */
    suspend fun getItem(itemBarcodeType: BarcodeType.Item?, customerOrderNumber: String?): ItemActivityDto?

    /** Returns the item matching the given [itemBpnId] and [customerOrderNumber] or null. */
    suspend fun getItem(itemBpnId: String?, customerOrderNumber: String?): ItemActivityDto?

    /** Returns the tote matching the given [toteBarcodeType] or null. */
    suspend fun getTote(toteBarcodeType: PickingContainer): ContainerActivityDto?

    /**
     * Returns a tote that matches given [item] of the same StorageType and matching customerOrderNumber (searching backwards through the list)
     *
     * Can be used to provide a hint to the picker indicating which tote to place the scanned item into.
     */
    suspend fun findExistingValidToteForItem(item: ItemActivityDto): ContainerActivityDto?

    /** True if tote and item 1) order number and 2) storage type match. Otherwise, false. */
    suspend fun isItemIntoPickingContainerValid(
        item: ItemActivityDto?,
        toteBarcodeType: PickingContainer,
        usesMFCTote: Boolean,
    ): Boolean

    // TODO: Master-order-view alternate name loadActivityDetailMasterViewData will decide
    suspend fun getActivityDetailsForSwapSubstitution(id: String, loadMasterView: Boolean?): ApiResult<ActivityDto>
    suspend fun isOtherPickerActive(id: String, itemId: String?, orderNumber: String?): ApiResult<ActivityDto>

    /** Clears out all memory/disk cached data (likely due to logout) */
    suspend fun clearAllData()
}

internal class PickRepositoryImplementation(
    private val app: Application,
    private val moshi: Moshi,
    private val offlinePickFile: File,
    private val apsService: ApsService,
    private val barcodeMapper: BarcodeMapper,
    private val pickListProcessor: PickListProcessor,
    private val pickListOperations: PickListOperations,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
    private val devOptionsRepository: DevOptionsRepository,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val toaster: Toaster,
    private val dispatcherProvider: DispatcherProvider,
    private val albApplicationCoroutineScope: AlbApplicationCoroutineScope,
    private val siteRepository: SiteRepository,
    private val conversationsRepository: ConversationsRepository,
    private val messagesRepository: MessagesRepository,
    private val conversationsClientWrapper: ConversationsClientWrapper,
    private val osccRepository: OsccRepository,
    private val itemProcessorRepository: ItemProcessorRepository,
    private val sharedPrefs: SharedPreferences,
) : PickRepository {

    private val lastSubOrOosTimeJsonAdapter = LastSubOrOosTimeJsonAdapter(moshi)
    private val _picklist: MutableStateFlow<ActivityDto?> = MutableStateFlow(null)
    private val _masterPickList: MutableStateFlow<ActivityDto?> = MutableStateFlow(null)
    private val _iaIdAfterSwapSubstitution: MutableStateFlow<Long?> = MutableStateFlow(null)
    private val _countDownTimer = MutableStateFlow(0)

    override val pickList: StateFlow<ActivityDto?>
        get() = _picklist
    override val masterPickList: StateFlow<ActivityDto?>
        get() = _masterPickList

    override val iaIdAfterSwapSubstitution: StateFlow<Long?>
        get() = _iaIdAfterSwapSubstitution
    override val countDownTimer: StateFlow<Int>
        get() = _countDownTimer

    private var countdownJob: Job? = null

    override val syncError = LiveEvent<Unit>()

    /** Represents a known pick list id that the current user has assigned to them, even if the full pick list details have not been queried/stored for offline use yet. */
    private var activePickListActivityId: String? = null

    /** Represents the activity details when the picklist was selected to be worked on this session of the app */
    private var baselineActivityDetails: ActivityDto? = null

    /** Represents the itemUpcs when the picklist was selected to be worked on this session of the app */
    private val itemUpcs: MutableList<ItemUpcDto> = mutableListOf()
    private val upcToItemIdMap = hashMapOf<String, String>()

    /** Represents the suggested substitution items associated with the items of the pick list */
    private val substitutionItemDetailList: MutableList<SubstitutionItemDetailsDto> = mutableListOf()

    /** Represents the alternate location items associated with the items of the pick list */
    private val alternateLocationsMap: MutableMap<String, List<ItemLocationDto>> = mutableMapOf()

    /** Source of truth for in-memory pick data has been successfully transferred to the backend but not yet present in the baseLineActivityDetails cached call.
     * Clear out the instance (or the lists) when fetching the latest activity details */
    private var onlineInMemoryPickData: OnlineInMemoryPickData = OnlineInMemoryPickData()

    /** Represents the offline pick data **/
    private var offlinePickData: OfflinePickData? = null

    /**
     * Use to lock critical code blocks that should never be accessed simultaneously (like reading from/writing to disk).
     * See https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#mutual-exclusion
     */
    private val offlineDataMutex = Mutex()

    /** True when a sync has been initiated. False when a sync completes (success or failure) */
    private var isSyncing = false
    private var syncStartTimestamp: Instant? = null

    init {
        albApplicationCoroutineScope.launch(dispatcherProvider.IO) {
            Timber.v("[init] reading initial offline state")
            readFromDisk()
            updatePickListState()

            restartTimerOnAppKills(_picklist.value?.actId?.toString() ?: "")

            // Listen to new "connected" network availability events
            networkAvailabilityManager.isConnected.filter { connected -> connected == true }.collect { connected ->
                // TODO: Should gate future logic/actions while syncing (add to a queue?)
                attemptSyncIfAble()
            }
        }
    }

    private fun restartTimerOnAppKills(actId: String) {
        getLastSubOrOutOfStockTime(actId)?.let {
            val elapsedTime = getTimeElapsedSinceLastSubOrOosInSeconds(it, ZonedDateTime.now())
            if (elapsedTime <= getTimerDelayForStaging()) {
                startCountdown(getTimerDelayForStaging() - elapsedTime.toInt())
            }
        }
    }

    private fun getTimeElapsedSinceLastSubOrOosInSeconds(startTime: ZonedDateTime, endTime: ZonedDateTime): Long {
        val seconds: Long = startTime.toInstant().epochSecond - endTime.toInstant().epochSecond
        return abs(seconds)
    }

    /**
     * Determines if timer operations should be performed based on the pick list type,
     * the status of the chatBeta flag, and the presence of non-customer suggested substitutions.
     * The timer will not start if only customer-suggested substitutions are picked.
     */
    private fun shouldPerformTimerOperations(): Boolean {
        val pickList = _picklist.value
        val chatBetaEnabled = siteRepository.twoWayCommsFlags.chatBeta == true
        val startTimer = pickList?.isStartTimerEnabled() ?: false
        val startTimerOrderNumberList = _picklist.value?.getListOfStartTimerOrderNumber().orEmpty().ifEmpty { listOf(_picklist.value?.customerOrderNumber.orEmpty()) }

        return pickList?.prePickType?.let { prePickType ->
            chatBetaEnabled && startTimer && !prePickType.isAdvancePickOrPrePick() &&
                (hasPicklistAnyNonCustomerSuggestedSubstitution(startTimerOrderNumberList) || hasAnyOosItems(startTimerOrderNumberList))
        } ?: (
            chatBetaEnabled && startTimer &&
                (hasPicklistAnyNonCustomerSuggestedSubstitution(startTimerOrderNumberList) || hasAnyOosItems(startTimerOrderNumberList))
            )
    }

    /**
     * Validating if any non customer suggested substitution has been picked through based on given list of OrderNumbers.
     */
    private fun hasPicklistAnyNonCustomerSuggestedSubstitution(startTimerOrderNumberList: List<String>): Boolean =
        _picklist.value?.itemActivities
            ?.filter { itemActivityDto -> itemActivityDto.customerOrderNumber in startTimerOrderNumberList }
            ?.any { it.hasAnySubstitutionOtherThanCustomerSuggestedSub() } == true

    /**
     * Validating if any item in the picklist is fully shorted based on given list of OrderNumbers..
     */
    private fun hasAnyOosItems(startTimerOrderNumberList: List<String>): Boolean =
        _picklist.value?.itemActivities
            ?.filter { itemActivityDto -> itemActivityDto.customerOrderNumber in startTimerOrderNumberList }
            ?.any { it.isFullyShorted() } == true

    /**
     * If a non-customer suggested substitution occur first,
     * the timer will start after recordItemPickComplete.
     * After that a customer suggested substitution occur later within 2 minutes,
     * the new timer will not restart, previous timer will resume.
     */
    private fun validateJustSubstitutedItemIsNonCustomerSuggested(item: ItemActivityDto?): Boolean =
        item?.hasAnySubstitutionOtherThanCustomerSuggestedSub() == true

    /**
     * Stop staging block timer if all non customer suggested substitution are repicked with orignal item or
     * substituted with customer suggested substitution if applicable with in 2 mins.
     */
    private fun stopTimerIfRequired() {
        val listOfStartTimerOrderNumber = _picklist.value?.getListOfStartTimerOrderNumber() ?: listOf(_picklist.value?.customerOrderNumber ?: "")
        val shouldStopSTimer = countdownJob?.isActive == true &&
            hasPicklistAnyNonCustomerSuggestedSubstitution(listOfStartTimerOrderNumber).not() &&
            hasAnyOosItems(listOfStartTimerOrderNumber).not()
        if (shouldStopSTimer) {
            _countDownTimer.value = 0
            countdownJob?.cancel()
        }
    }

    private fun stopTimer() {
        _countDownTimer.value = 0
        countdownJob?.cancel()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startCountdown(seconds: Int) {
        if (shouldPerformTimerOperations()) {
            Timber.d("Starting countdown timer $seconds")
            countdownJob?.cancel()
            countdownJob = GlobalScope.launch {
                var countdownTime = seconds // Convert minutes to seconds
                while (countdownTime >= 0) {
                    _countDownTimer.value = countdownTime
                    countdownTime--
                    delay(1000)
                }
            }
        }
    }

    private fun saveLastSubOrOutOfStockTime(actId: String, zonedDateTime: ZonedDateTime) {
        if (shouldPerformTimerOperations()) {
            with(sharedPrefs.edit()) {
                val jsonObj = lastSubOrOosTimeJsonAdapter.toJson(LastSubOrOosTime(zonedDateTime))
                putString(actId, jsonObj)
                commit()
            }
        }
    }

    fun getLastSubOrOutOfStockTime(orderNumber: String): ZonedDateTime? {
        if (shouldPerformTimerOperations()) {
            if (sharedPrefs.contains(orderNumber)) {
                return sharedPrefs.getString(orderNumber, String())
                    ?.let { lastSubOrOosTimeJsonAdapter.fromJson(it)?.lastSubOrOosTime }
            }
        }
        return null
    }

    private fun clearLastSubOrOutOfStockTime() {
        if (shouldPerformTimerOperations()) {
            sharedPrefs.edit().clear().apply()
        }
    }

    /** Kicks off a sync api call if [shouldAttemptSync] is true. */
    private suspend fun attemptSyncIfAble(connectedArg: Boolean? = null) {
        val connected = connectedArg ?: networkAvailabilityManager.isConnected.first()
        if (shouldAttemptSync(connected)) {
            isSyncing = true
            syncStartTimestamp = Instant.now()
            Timber.v("[init] connected and needs to sync - initiating sync")
            offlinePickData?.let { syncOfflineOperations(it.toSyncOfflinePickingRequestDto()) }
        }
    }

    /** True when device online and sync data present. */
    private suspend fun shouldAttemptSync(connected: Boolean): Boolean {
        val syncDataPresent = isSyncDataPresent()
        Timber.v("[shouldAttemptSync] connected=$connected, syncDataPresent=$syncDataPresent")
        return connected && isSyncDataPresent()
    }

    override fun setActivePickListActivityId(activityId: String) {
        activePickListActivityId = activityId
    }

    override fun getActivePickListActivityId(): String? {
        return activePickListActivityId
    }

    override fun hasActivePickListActivityId(): Boolean = activePickListActivityId.isNotNullOrEmpty()

    override suspend fun hasOfflinePickListData(): Boolean = pickList.first() != null

    override suspend fun assignUser(assignUserRequestDto: AssignUserRequestDto): ApiResult<ActivityDto> {
        return wrapExceptions("assignUser") {
            apsService.assignUser(assignUserRequestDto).toResult()
                .alsoOnSuccess { activityDto ->
                    if (conversationsClientWrapper.isClientCreated) {
                        coroutineScope {
                            activityDto.orderChatDetails?.mapNotNull { orderChatDetail ->
                                orderChatDetail.conversationSid?.takeIf { it.isNotNullOrEmpty() }?.let { conversationSid ->
                                    async {
                                        addParticipantToConversation(conversationSid, assignUserRequestDto.user?.userId, orderChatDetail.customerOrderNumber)
                                    }
                                }
                            }?.awaitAll()
                        }
                    }

                    resetBaseline(activityDto)
                    updatePickListState()
                    setActivePickListActivityId(activityDto.actId?.toString() ?: "")
                }
        }
    }

    private suspend fun addParticipantToConversation(
        conversationId: String,
        userId: String?,
        customerOrderNumber: String?,
    ) {
        val participantList = conversationsRepository.fetchParticipants(conversationId)
        val isAdded = participantList?.any { it.identity == userId } == true
        if (!isAdded && siteRepository.twoWayCommsFlags.chatBeta == true && userId != null) {
            osccRepository.addParticipant(conversationId, userId)
                .alsoOnFailure { result ->
                    if (result is ApiResult.Failure) {
                        val chatErrorData = ChatErrorData(
                            orderNumbers = listOfNotNull(customerOrderNumber),
                            conversationSids = listOf(conversationId),
                            errorMessage = result.toString()
                        ).toJsonString(moshi)
                        val error = ApiResult.Failure.GeneralFailure(
                            chatErrorData,
                            networkCallName = NetworkCalls.ADD_PARTICIPANT_FAILURE.value
                        )
                        conversationsRepository.sendLog(error, false)
                    }
                }
        }
    }

    override suspend fun reAssignUserStaging(user: ReassignDropOffRequestDto): ApiResult<Unit> {
        return wrapExceptions("reAssignUser") {
            apsService.reAssignUserStaging(reAssign = user).toEmptyResult()
        }
    }

    override suspend fun unAssignUser(actId: String, userId: String?, tokenizedLdap: String, orderIds: List<String>?): ApiResult<Unit> {
        return wrapExceptions("unAssignUser") {
            clearLastSubOrOutOfStockTime()
            apsService.unAssignUser(actId = actId).toResult().alsoOnSuccess {
                userId?.let {
                    orderIds?.map { orderId ->
                        if (conversationsClientWrapper.isClientCreated) {
                            val conversationId = conversationsRepository.getConversationId(orderId)
                            if (conversationId.isNotNullOrEmpty()) {
                                messagesRepository.sendPickerLeftMessage(conversationId, orderId, it, tokenizedLdap)
                                conversationsRepository.removeParticipant(conversationId, userId)
                            }
                        }
                    }
                }
                clearAllData()
                itemProcessorRepository.clearItemProcessorData()
            }
        }
    }

    override suspend fun getActivityDetails(id: String, shouldUpdatePickListState: Boolean): ApiResult<ActivityDto> {
        return wrapExceptions("getActivityDetails") {
            if (networkAvailabilityManager.isConnected.first()) {
                apsService.getActivityDetails(id = id, loadCa = true, loadIa = true).toResult().alsoOnSuccess {
                    if (shouldUpdatePickListState) {
                        resetBaseline(it)
                        updatePickListState()
                        checkForStopTimer(it)
                    }
                }
            } else {
                baselineActivityDetails?.let { ApiResult.Success(baselineActivityDetails!!) } ?: ApiResult.Failure.GeneralFailure("getActivityDetails: baseline is null")
            }
        }
    }

    private fun checkForStopTimer(activityDto: ActivityDto) {
        if (areAllSubsApproved(activityDto)) {
            stopTimer()
        }
    }

    private fun areAllSubsApproved(activityDto: ActivityDto): Boolean {
        Timber.d("approve areAllSubsApproved==")
        return activityDto.itemActivities
            ?.filter { itemActivityDto -> itemActivityDto.pickedUpcCodes.isNotNullOrEmpty() && itemActivityDto.pickedUpcCodes?.any { it.isSubstitutionOrIssueScanning() } == true }
            ?.all { item ->
                item.pickedUpcCodes
                    ?.firstOrNull { it.isSubstitutionOrIssueScanning() }
                    ?.let { it.isRejected == false } == true
            } == true && !activityDto.itemActivities.any { (it.exceptionQty ?: 0.0) > 0.0 }
    }

    override suspend fun completePickForStaging(pickReq: PickCompleteRequestDto, tokenizedLdap: String): ApiResult<ActivityDto> {
        return wrapExceptions("completePickForStaging") {
            clearLastSubOrOutOfStockTime()
            apsService.completePickForStaging(pickReq).toResult().alsoOnSuccess { activity ->
                pickReq.userId?.let { userId ->
                    if (conversationsClientWrapper.isClientCreated) {
                        activity.orderChatDetails?.forEach { orderChatDetail ->
                            orderChatDetail.conversationSid?.takeIf { it.isNotNullOrEmpty() }?.let { conversationSid ->
                                messagesRepository.sendPickerLeftMessage(conversationSid, orderChatDetail.customerOrderNumber.orEmpty(), userId, tokenizedLdap)
                                conversationsRepository.removeParticipant(conversationSid, userId)
                            }
                        }
                    } else if (siteRepository.twoWayCommsFlags.chatBeta == true && activity.getListOfConversationSid().isNotNullOrEmpty()) {
                        val errorData = ChatErrorData(
                            orderNumbers = activity.getListOfOrderNumber(),
                            errorMessage = conversationsRepository.errorData.value.toString(),
                            conversationSids = activity.getListOfConversationSid()
                        ).toJsonString(moshi)
                        conversationsRepository.sendLog(
                            ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.PICKED_WITH_SDK_FAILURE.value),
                            false
                        )
                    }
                }
            }
        }
    }

    override suspend fun getStagingSummary(actId: String): ApiResult<StagingSummaryDto> {
        return wrapExceptions("getStagingSummary") {
            apsService.getStagingSummary(actId).toResult()
        }
    }

    override suspend fun recordItemPickComplete(itemPickCompleteDto: ItemPickCompleteDto): ApiResult<Unit> {
        return wrapExceptions("recordItemPickComplete") {
            val networkStateSyncInstant = createNetworkStateSyncInstant()
            if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                apsService.recordItemPickComplete(itemPickCompleteDto).toResult().alsoOnSuccess {
                    val pickList = _picklist.value
                    val itemActivity = pickList?.itemActivities?.find { it.id == itemPickCompleteDto.iaId }
                    val orderChatDetail = pickList?.orderChatDetails?.find { it?.customerOrderNumber == itemActivity?.customerOrderNumber }
                    val isStartTimerEnabled = orderChatDetail?.startChatBlockTimer == true || pickList?.startTimer == true

                    if (isStartTimerEnabled && validateJustSubstitutedItemIsNonCustomerSuggested(itemActivity)) {
                        startCountdown(getTimerDelayForStaging())
                        saveLastSubOrOutOfStockTime(pickList?.actId?.toString() ?: "", ZonedDateTime.now())
                    }
                }
            } else {
                Timber.v("[recordItemPickComplete] offline pick - networkStateSyncInstant=$networkStateSyncInstant")
                writeToDisk(offlinePickData?.itemPickCompleteDto?.plus(itemPickCompleteDto)?.let { offlinePickData?.copy(itemPickCompleteDto = it) })
                updatePickListState()
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun getItemDetails(
        siteId: String,
        upcId: String,
        pluCode: String?,
        actId: Long?,
        originalItemId: String?,
        sellByWeightInd: String?,
        queryType: String?,
    ): ApiResult<ItemDetailDto> {
        return wrapExceptions("getItemDetails") {
            apsService.getItemDetails(siteId, upcId, pluCode, actId, originalItemId, sellByWeightInd, queryType).toResult()
        }
    }

    override suspend fun getSubstitutionItemDetailList(actId: String): ApiResult<List<SubstitutionItemDetailsDto>> {
        return wrapExceptions("getSubstitutionItemDetails") {
            apsService.getSubstitutionItemDetailList(actId).toResult().alsoOnSuccess { backendSubstitutionItemDetailsList ->
                substitutionItemDetailList.clear()
                substitutionItemDetailList.addAll(backendSubstitutionItemDetailsList)
                writeToDisk(offlinePickData?.copy(substitutionItemDetails = backendSubstitutionItemDetailsList))
            }
        }
    }

    override suspend fun getAllItemLocations(siteId: String, itemId: List<String>): ApiResult<Map<String, List<ItemLocationDto>>> {
        return wrapExceptions("getAlternateLocations") {
            apsService.getAllItemLocations(siteId, itemId).toResult().alsoOnSuccess { altLocationsMap ->
                alternateLocationsMap.clear()
                alternateLocationsMap.putAll(altLocationsMap)
                writeToDisk(offlinePickData?.copy(alternateLocationsDetailsMap = altLocationsMap))
            }
        }
    }

    override suspend fun getItemUpcList(siteId: String, itemIds: List<String>, itemActivities: List<ItemActivityDto>): ApiResult<List<ItemUpcDto>> {
        return wrapExceptions("getItemUpcList") {
            val substitutionItemIds = substitutionItemDetailList.map { it.smartSubItemDetails?.firstOrNull()?.itemId.orEmpty() }.distinct()
            val allItemIds = itemIds + substitutionItemIds
            apsService.getItemUpcList(siteId, allItemIds).toResult().alsoOnSuccess { backendItemUpcDtoList ->
                // Clear all existing item upcs and add new ones to the list
                itemUpcs.clear()
                // Appends primary upc to the list (if not present). **Do not assume the last value is in the [ItemUpcDto.upcList] is the primary UPC!**
                val combinedBackendAndPrimaryUpcItemUpcList = backendItemUpcDtoList.appendPrimaryUpcToItemUpcList(itemActivities)
                itemUpcs.addAll(combinedBackendAndPrimaryUpcItemUpcList)
                writeToDisk(offlinePickData?.copy(itemUpcs = combinedBackendAndPrimaryUpcItemUpcList))

                generateUpcToItemIdMap()
            }
        }
    }

    override suspend fun recordPick(request: PickRequest): ApiResult<Unit> {
        return pickList.first()?.let { pickList ->
            val itemPickRequestDto = ItemPickRequestDto(
                actId = pickList.actId,
                lineReqDto = listOf(
                    LineRequestDto(
                        containerId = request.toteBarcodeType.rawBarcode,
                        // lookup item based on bpn and customer order number (possible that the upc is not in the itemUpcList) See https://jira.bottlerocketapps.com/browse/ALBPK-419
                        iaId = getItem(request.itemBpnId, request.customerOrderNumber)?.id,
                        upcId = request.itemBarcodeType.getBarcodeToSendToBackend(),
                        catalogUpc = request.itemBarcodeType.catalogLookupUpc,
                        ignoreUpc = true, // Note: This value should always be set to true in the pick requests (backend validation deprecated - decision is to rely on front end validation)
                        pickedTime = ZonedDateTime.now(),
                        disableContainerValidation = request.disableContainerValidation,
                        fulfilledQty = request.fulfilledQuantity,
                        upcQty = request.upcQuantity,
                        userId = request.userId,
                        netWeight = request.netWeight,
                        scannedPrice = request.scannedPrice,
                        sellByWeightInd = request.sellByWeightInd,
                        storageType = request.storageType,
                        isManuallyEntered = request.isManuallyEntered
                    )
                )
            )
            recordPick(pickList.actId?.toString() ?: "", itemPickRequestDto, request.itemBarcodeType)
        } ?: ApiResult.Failure.GeneralFailure("recordPick - pickList is null")
    }

    override suspend fun recordSubstitution(request: SubstitutePickRequest): ApiResult<Unit> {
        return pickList.first()?.let { pickList ->
            val itemPickRequestDto = ItemPickRequestDto(
                actId = pickList.actId,
                lineReqDto = listOf(
                    LineRequestDto(
                        containerId = request.toteBarcodeType.rawBarcode,
                        disableContainerValidation = request.disableContainerValidation,
                        fulfilledQty = request.fulfilledQuantity,
                        iaId = request.originalItem.id,
                        ignoreUpc = true, // Note: This value should always be set to true in the pick requests (backend validation deprecated - decision is to rely on front end validation)
                        isSmartSubItem = request.isSmartSubItem,
                        originalItemId = request.originalItem.itemId,
                        pickedTime = ZonedDateTime.now(),
                        regulated = request.regulated,
                        catalogUpc = request.itemBarcodeType.catalogLookupUpc,
                        subReasonCode = request.subReasonCode,
                        substituteItemDesc = request.substituteItem.description,
                        substituteItemId = request.substituteItem.itemId,
                        substitution = !request.sameItemSubbed,
                        storageType = request.substituteItem.storageType,
                        upcId = request.substituteItem.modifiedUpc, // The documentation seems to suggest this should be the original UPC, but setting it to the substitute UPC seems more correct
                        upcQty = request.upcQuantity,
                        userId = request.userId,
                        scannedPrice = request.scannedPrice,
                        sameItemSubbed = request.sameItemSubbed,
                        exceptionDetailsId = request.exceptionDetailsId,
                        substitutionReason = request.substitutionReason,
                        sellByWeightInd = request.sellByWeightInd,
                        messageSid = request.messageSid,
                        isManuallyEntered = request.isManuallyEntered
                    )
                )
            )
            recordPick(pickList.actId?.toString() ?: "", itemPickRequestDto, request.itemBarcodeType)
        } ?: ApiResult.Failure.GeneralFailure("recordSubstitution - pickList is null")
    }

    private suspend fun recordPick(pickListId: String, itemPickRequestDto: ItemPickRequestDto, barcodeType: BarcodeType.Item): ApiResult<Unit> {
        return wrapExceptions("recordPick") {
            // Follow recommended approach to continue coroutine execution (even if parent CoroutineScope, likely viewmodelscope, has been cancelled). Using async as we need to return a result.
            // from https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
            @Suppress("RedundantAsync")
            albApplicationCoroutineScope.async(dispatcherProvider.IO) {
                val networkStateSyncInstant = createNetworkStateSyncInstant()
                if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                    Timber.v("[recordPick] online attempt")
                    apsService.recordPick(itemPickRequestDto).toResult().alsoOnSuccess { response ->
                        Timber.v("[recordPick] successful online pick")
                        /**
                         * Master-order-view: In case of swap substitution for other picker's
                         * We need to use new iaId in further process such as next recordPick, undoPick, recordItemPickComplete API's
                         */
                        if (itemPickRequestDto.lineReqDto?.first()?.substitutionReason?.shoulUseOtherPickersIaId() == true) {
                            _iaIdAfterSwapSubstitution.value = response.first().iaId
                        }
                        onlineInMemoryPickData = onlineInMemoryPickData.copy(itemPickRequestDtos = onlineInMemoryPickData.itemPickRequestDtos + RequestResponse(itemPickRequestDto, response))
                        updatePickListState()
                        // Moved from viewmodel down to repo layer here since it is CRUCIAL that activity details be reloaded (when not using in memory state)
                        // to properly represent UI/allowed logic for successful picks
                        if (!devOptionsRepository.useOnlineInMemoryPickListState) {
                            getActivityDetails(pickListId)
                        }
                    }.asEmptyResult()
                } else {
                    Timber.v("[recordPick] offline pick - networkStateSyncInstant=$networkStateSyncInstant")

                    val itemPickRequests: MutableList<ItemPickRequestDto> = mutableListOf()

                    val isItemWeighted = barcodeType.isItemWeighted()
                    val isItemPriced = barcodeType.isItemPriced() && siteRepository.fixedItemTypesEnabled
                    if (isItemWeighted || isItemPriced || itemPickRequestDto.lineReqDto.isNullOrEmpty()) {
                        itemPickRequests.add(itemPickRequestDto)
                    } else if (barcodeType.isPriceWeighted() || barcodeType.isPriceEach()) {
                        itemPickRequests.add(itemPickRequestDto)
                    } else {
                        for (i in 1..itemPickRequestDto.lineReqDto.first().upcQty!!.toInt()) {
                            itemPickRequests.add(
                                itemPickRequestDto.copy(lineReqDto = listOf(itemPickRequestDto.lineReqDto.first().copy(upcQty = 1.0, fulfilledQty = 1.0)))
                            )
                        }
                    }
                    writeToDisk(offlinePickData?.itemPickRequestDtos?.plus(itemPickRequests)?.let { offlinePickData?.copy(itemPickRequestDtos = it) })
                    updatePickListState()
                    ApiResult.Success(Unit)
                }
            }.await()
        }
    }

    override suspend fun completeClickedCall(itemId: String): ApiResult<Unit> {
        return wrapExceptions("completeClickedCall") {
            // Follow recommended approach to continue coroutine execution (even if parent CoroutineScope, likely viewmodelscope, has been cancelled). Using async as we need to return a result.
            // from https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
            @Suppress("RedundantAsync")
            albApplicationCoroutineScope.async(dispatcherProvider.IO) {
                val networkStateSyncInstant = createNetworkStateSyncInstant()
                if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                    Timber.v("[completeClickedCall] online attempt")
                    apsService.completePickClicked(itemId).toResult().alsoOnSuccess { response ->
                        // if (!devOptionsRepository.useOnlineInMemoryPickListState) {
                        //     getActivityDetails(pickListId)
                        // }
                    }
                } else {
                    Timber.v("[completeClickedCall] offline pick - networkStateSyncInstant=$networkStateSyncInstant")

                    // cahnge flag to for isPickCompleted for matching items
                    val newOfflinePickData = offlinePickData?.itemPickRequestDtos?.map { itemPickRequestDtos ->
                        if (itemPickRequestDtos.lineReqDto?.any { it.iaId == itemId.toLongOrNull() } != false) {
                            val modifiedLine = itemPickRequestDtos.lineReqDto?.map { line ->
                                line.copy(isPickCompleted = true)
                            }
                            itemPickRequestDtos.copy(lineReqDto = modifiedLine)
                        } else itemPickRequestDtos
                    }

                    writeToDisk(offlinePickData?.let { newOfflinePickData?.let { offlinePickData?.copy(itemPickRequestDtos = newOfflinePickData) } })
                    ApiResult.Success(Unit)
                }
            }.await()
        }
    }

    override suspend fun recordShortage(shortPickRequestDto: ShortPickRequestDto): ApiResult<Unit> {
        return wrapExceptions("recordShortage") {
            val networkStateSyncInstant = createNetworkStateSyncInstant()
            if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                Timber.v("[recordShortage] online attempt")
                // FIXME: Do we need to wrap this since there is already a short timestamp inside the object?
                // val wrappedRequest = shortPickRequestDto.wrapActionTime()
                apsService.recordShortage(shortPickRequestDto).toResult().alsoOnSuccess { response ->
                    Timber.v("[recordShortage] successful online pick")
                    onlineInMemoryPickData = onlineInMemoryPickData.copy(shortPickRequestDtos = onlineInMemoryPickData.shortPickRequestDtos + RequestResponse(shortPickRequestDto, response))
                    updatePickListState()
                    if (!devOptionsRepository.useOnlineInMemoryPickListState) {
                        getActivityDetails(shortPickRequestDto.actId.toString())
                    }
                    // start timer for subs or out of stock
                    if (shortPickRequestDto.shortReqDto?.firstOrNull()?.shortageReasonCode.shouldStartCountDownTimer()) {
                        startCountdown(getTimerDelayForStaging())
                        saveLastSubOrOutOfStockTime(shortPickRequestDto.actId?.toString() ?: "", ZonedDateTime.now())
                    }
                }.asEmptyResult()
            } else {
                Timber.v("[recordShortage] offline pick - networkStateSyncInstant=$networkStateSyncInstant")
                writeToDisk(offlinePickData?.shortPickRequestDtos?.plus(shortPickRequestDto)?.let { offlinePickData?.copy(shortPickRequestDtos = it) })
                updatePickListState()
                ApiResult.Success(Unit)
            }
        }
    }

    /** Note that this function should only be called from [attemptSyncIfAble] (as values are set there) */
    private suspend fun syncOfflineOperations(syncOfflinePickingRequestDto: SyncOfflinePickingRequestDto): ApiResult<ActivityDto> {

        fun hasSubsOrOutOfStock(): Boolean {
            return syncOfflinePickingRequestDto.recordPickDto?.any { line -> line.lineReqDto?.any { it.substitution == true } == true } == true ||
                syncOfflinePickingRequestDto.recordShortage.isNotNullOrEmpty() && syncOfflinePickingRequestDto.recordShortage
                ?.any { shortPickRequestDto -> shortPickRequestDto.shortReqDto?.any { it.shortageReasonCode.shouldStartCountDownTimer() } == true } == true
        }

        return wrapExceptions("syncOfflineOperations") {
            apsService.syncOfflineOperations(syncOfflinePickingRequestDto).toResult()
        }.also {
            when (it) {

                is ApiResult.Success -> {
                    Timber.v("[syncOfflineOperations] successful sync! Resetting baseline activity details and removing offline data")
                    // If successfully synced:
                    // 1) clear out the synced offline cache (it is no longer necessary)
                    // 2) reset baseline
                    // 3) update the pick list state
                    // 4) Sync any actions performed while this sync was in progress (optionally run)
                    writeToDisk(offlinePickData?.copyWithUnsyncedOfflineActionsCleared(ZonedDateTime.ofInstant(syncStartTimestamp, ZoneId.systemDefault())))
                    resetBaseline(it.data)
                    updatePickListState()
                    if (hasSubsOrOutOfStock()) {
                        startCountdown(getTimerDelayForStaging())
                        saveLastSubOrOutOfStockTime(syncOfflinePickingRequestDto.actId?.toString() ?: "", ZonedDateTime.now())
                    }
                    attemptSyncIfAble()
                }

                is ApiResult.Failure.NetworkFailure -> {
                    // TODO: Validate retry delay time and logic is appropriate.
                    // FIXME: Add items to the offline queue until it is successfully synced or fails and is cleared
                    Timber.d("[syncOfflineOperations] network failure on sync - attempting sync again shortly")
                    delay(SYNC_NETWORK_TIMEOUT_FAILURE_NEXT_SYNC_DELAY_DURATION.toMillis())
                    attemptSyncIfAble()
                }

                is ApiResult.Failure -> {
                    Timber.w("[syncOfflineOperations] unrecoverable error with sync! Removing offline pick data")
                    withContext(dispatcherProvider.Main) {
                        // Note: UI update triggered here due to the automatic nature of syncing (no user actions coming down from viewmodel.
                        syncError.postValue(Unit)
                    }
                    // If an unrecoverable failure type on the backend, 1) clear out the offline cache (it is unusable, no recovery available) 2) update the picklist state
                    writeToDisk(offlinePickData?.copyWithAllOfflineActionsCleared())
                    updatePickListState()
                }
            }

            isSyncing = false
            syncStartTimestamp = null
        }
    }

    override suspend fun undoPicks(requestList: List<UndoPickLocalDto>): ApiResult<Unit> {
        val combinedList = if (createNetworkStateSyncInstant().isOnlineAndNotSyncing()) requestList.combineIdenticalUndoPickLocalDtos() else requestList
        val result = undoPick(combinedList)
        if (!devOptionsRepository.useOnlineInMemoryPickListState) {
            getActivityDetails(requestList.first().undoPickRequestDto.actId.toString())
        }
        return result
    }

    private suspend fun undoPick(undoPickRequestDtoList: List<UndoPickLocalDto>): ApiResult<Unit> {

        return wrapExceptions("undoPick") {
            val networkStateSyncInstant = createNetworkStateSyncInstant()
            val listOfRequestDto = mutableListOf<UndoPickRequestDto>()
            val wrappedRequestList = mutableListOf<ActionTimeWrapper<UndoPickLocalDto>>()
            undoPickRequestDtoList.forEach {
                listOfRequestDto.add(it.undoPickRequestDto)
                wrappedRequestList.add(it.wrapActionTime())
            }

            if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                Timber.v("[undoPick] online attempt")
                apsService.undoPicks(listOfRequestDto).toResult().alsoOnSuccess { response ->
                    /** onlineInMemoryPickData is not used in debug / production build, so below line is commented.
                     In debug build by default we are not using this data. Only through dev option we can enable useOnlineInMemoryPickListState to use this data.
                     As per jira ticket "ACIP-278405 undo picks for live order view", we have consolidated multiple undos into single api call.
                     if you want to use onlineInMemoryPickData in future please update type of undoItemPickRequestDtos to List<UndoPickLocalDto> to
                     */
                    // onlineInMemoryPickData = onlineInMemoryPickData.copy(undoItemPickRequestDtos = onlineInMemoryPickData.undoItemPickRequestDtos + RequestResponse(wrappedRequest, response))
                    updatePickListState()
                }.asEmptyResult()
            } else {
                Timber.v("[undoPick] offline undoPick - networkStateSyncInstant=$networkStateSyncInstant")
                wrappedRequestList.forEach { wrappedRequest ->
                    writeToDisk(offlinePickData?.undoItemPickRequestDtos?.plus(wrappedRequest)?.let { offlinePickData?.copy(undoItemPickRequestDtos = it) })
                }
                updatePickListState()
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun undoShortages(requestList: List<UndoShortRequestDto>): Map<UndoShortRequestDto, ApiResult<Unit>> {
        val result = mutableMapOf<UndoShortRequestDto, ApiResult<Unit>>()
        requestList.forEach { request ->
            result[request] = undoShortage(request)
        }
        if (!devOptionsRepository.useOnlineInMemoryPickListState && requestList.isNotEmpty()) {
            getActivityDetails(requestList.first().actId.toString())
        }
        return result
    }

    override suspend fun undoShortage(undoShortRequestDto: UndoShortRequestDto): ApiResult<Unit> {
        return wrapExceptions("undoShortage") {
            val wrappedRequest = undoShortRequestDto.wrapActionTime()
            val networkStateSyncInstant = createNetworkStateSyncInstant()
            if (networkStateSyncInstant.isOnlineAndNotSyncing()) {
                Timber.v("[undoShortage] online attempt")
                apsService.undoShortage(undoShortRequestDto).toResult().alsoOnSuccess { response ->
                    Timber.v("[undoShortage] successful online undo short")
                    onlineInMemoryPickData = onlineInMemoryPickData.copy(undoShortRequestDtos = onlineInMemoryPickData.undoShortRequestDtos + RequestResponse(wrappedRequest, response))
                    updatePickListState()
                }.asEmptyResult()
            } else {
                Timber.v("[undoShortage] offline undoShortage - networkStateSyncInstant=$networkStateSyncInstant")
                writeToDisk(offlinePickData?.undoShortRequestDtos?.plus(wrappedRequest)?.let { offlinePickData?.copy(undoShortRequestDtos = it) })
                updatePickListState()
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun getItemId(itemBarcodeType: BarcodeType.Item?): String? = pickListOperations.getItemId(getItemActivities(), upcToItemIdMap, itemBarcodeType)

    override fun getSubstitutionItemDetails(iaId: Long): ItemActivityDto? = substitutionItemDetailList.firstOrNull { it.iaId == iaId }?.smartSubItemDetails?.firstOrNull()

    override fun getAlternateLocations(itemId: String): List<ItemLocationDto>? {
        val locationsDtoList: List<ItemLocationDto>? = alternateLocationsMap[itemId]
        return locationsDtoList?.filter { location -> location.primary == false }?.take(2)
    }

    override suspend fun getItemWithoutOrderOrCustomerDetails(itemBarcodeType: BarcodeType.Item?): ItemSearchResult {
        return pickListOperations.getItemWithoutOrderOrCustomerDetails(getItemActivities(), upcToItemIdMap, itemBarcodeType)
    }

    override suspend fun getNextItemToSelectForScan(itemBarcodeType: BarcodeType.Item?, currentSelectedItem: ItemActivityDto?): ItemSearchResult {
        return pickListOperations.getNextItemToSelectForScan(getItemActivities(), upcToItemIdMap, itemBarcodeType, currentSelectedItem)
    }

    override suspend fun getItem(itemBarcodeType: BarcodeType.Item?, customerOrderNumber: String?): ItemActivityDto? {
        return pickListOperations.getItem(getItemActivities(), upcToItemIdMap, itemBarcodeType, customerOrderNumber)
    }

    override suspend fun getItem(itemBpnId: String?, customerOrderNumber: String?): ItemActivityDto? =
        pickListOperations.getItem(getItemActivities(), itemBpnId, customerOrderNumber)

    override suspend fun getTote(toteBarcodeType: PickingContainer): ContainerActivityDto? =
        pickListOperations.getTote(getContainerActivities(), toteBarcodeType)

    override suspend fun findExistingValidToteForItem(item: ItemActivityDto): ContainerActivityDto? {
        return pickListOperations.findExistingValidToteForItem(getContainerActivities(), item)
    }

    override suspend fun isItemIntoPickingContainerValid(item: ItemActivityDto?, toteBarcodeType: PickingContainer, isMFCTote: Boolean): Boolean {
        return pickListOperations.isItemIntoPickingContainerValid(item, getContainerActivities(), toteBarcodeType, isMFCTote)
    }

    override suspend fun getActivityDetailsForSwapSubstitution(id: String, loadMasterView: Boolean?): ApiResult<ActivityDto> {
        return wrapExceptions("getActivityDetailsForSwapSubstitution") {
            apsService.getActivityDetails(id = id, loadCa = true, loadIa = true, loadMasterView = loadMasterView).toResult()
        }
    }

    override suspend fun isOtherPickerActive(id: String, itemId: String?, orderNumber: String?): ApiResult<ActivityDto> {
        return wrapExceptions("isOtherPickerActiveInSwapSubstitution") {
            apsService.getActivityDetails(id = id, loadCa = true, loadIa = true, itemId = itemId, orderNumber = orderNumber).toResult().alsoOnSuccess { activityDto ->
                _masterPickList.value = activityDto
            }
        }
    }

    /** True if the matching pick list item has weight as the sell by type OR if the barcode type is weighted (when the item is not in the pick list, for substitutions) */
    private suspend fun BarcodeType.Item.isItemWeighted(): Boolean = getItemWithoutOrderOrCustomerDetails(this).isWeightedItem() || this is BarcodeType.Item.Weighted
    private suspend fun BarcodeType.Item.isItemPriced(): Boolean = this is BarcodeType.Item.Priced &&
        getItemWithoutOrderOrCustomerDetails(this).getItemActivityDto()?.sellByWeightInd != SellByType.PriceEachUnique

    private suspend fun BarcodeType.Item.isPriceWeighted(): Boolean = getItemWithoutOrderOrCustomerDetails(this).getItemActivityDto()?.sellByWeightInd == SellByType.PriceWeighted
    private suspend fun BarcodeType.Item.isPriceEach(): Boolean = getItemWithoutOrderOrCustomerDetails(this).getItemActivityDto()?.sellByWeightInd == SellByType.PriceEach

    private suspend fun getItemActivities(): List<ItemActivityDto> = pickList.first()?.itemActivities.orEmpty()
    private suspend fun getContainerActivities(): List<ContainerActivityDto> = pickList.first()?.containerActivities.orEmpty()

    private fun generateUpcToItemIdMap() {
        val primaryitemIds: MutableList<String?> = mutableListOf()
        pickList.value?.itemActivities?.forEach {
            primaryitemIds.add(it.itemId)
        }
        itemUpcs.let {
            // build a map of upc -> itemId
            it.forEach { itemId ->
                itemId.upcList?.forEach { upc ->
                    // map UPC to itemId if it is not already added or if the added itemId is not primary itemId's
                    if (upcToItemIdMap.get(upc).isNullOrEmpty() || !primaryitemIds.contains(upcToItemIdMap.get(upc))) {
                        upcToItemIdMap[upc] = itemId.itemId ?: ""
                    }
                }
            }
        }
    }

    private fun getTimerDelayForStaging(): Int {
        return if (pickList.value?.isPrepNeeded == true) {
            siteRepository.prepNotReadyDelayTime
        } else {
            siteRepository.realTimeSubDelay
        } * 60 // convert minutes to seconds
    }

    override suspend fun clearAllData() {
        offlineDataMutex.withLock {
            Timber.v("[clearAllData]")
            _picklist.value = null
            activePickListActivityId = null
            baselineActivityDetails = null
            itemUpcs.clear()
            upcToItemIdMap.clear()
            _masterPickList.value = null
            _iaIdAfterSwapSubstitution.value = null
            onlineInMemoryPickData = OnlineInMemoryPickData()
            // Removes all memory and disk representations of offline pick data
            offlinePickData = null
            offlinePickFile.delete()
            clearLastSubOrOutOfStockTime()
        }
    }

    /** Wrapper class that holds both online and sync values that can be used for logging */
    data class NetworkStateSyncInstant(val isOnline: Boolean, val isSyncing: Boolean) {
        /** True when online and not syncing. False if offline or syncing. */
        fun isOnlineAndNotSyncing(): Boolean = isOnline && !isSyncing
    }

    private suspend fun isOnline(): Boolean = networkAvailabilityManager.isConnected.first()

    private suspend fun createNetworkStateSyncInstant(): NetworkStateSyncInstant = NetworkStateSyncInstant(isOnline(), isSyncing)

    /** True if any of the record pick/short undo pick/short arrays. */
    private fun isSyncDataPresent(): Boolean = offlinePickData?.isSyncDataPresent() ?: false

    /** When activity details is updated, use [newBaseline] as the new baseline and reset [onlineInMemoryPickData] since it should be represented by [newBaseline] now */
    private suspend fun resetBaseline(newBaseline: ActivityDto) {
        Timber.v("[resetBaseline]")
        baselineActivityDetails = newBaseline
        // Since we have a new baseline, all of the online in memory data needs to be cleared (as it is already accounted for in the new baseline)
        onlineInMemoryPickData = OnlineInMemoryPickData()
        // All offline data is dependent on activity details;
        // Update the baseline pick list when offlinePickData is not null OR create a new instance of offlinePickData if it is currently null (not loaded from disk)
        writeToDisk(offlinePickData?.copy(baselineActivityDetails = newBaseline) ?: OfflinePickData(baselineActivityDetails = newBaseline))
    }

    /**
     * Updates picker display pick list state logic from a combination of:
     * * last activity details source of truth pick list state +
     * * online picking activity (not yet reflected in last activity details source of truth) +
     * * offline picking activity (not yet synced)
     */
    @SuppressLint("BinaryOperationInTimber")
    private fun updatePickListState() {
        baselineActivityDetails?.let {
            if (offlinePickData != null) {
                val unifiedPickListState = pickListProcessor.processUnifiedPickListState(
                    PickListProcessorInput(
                        baselineActivityDetails = it,
                        onlineInMemoryPickData = if (devOptionsRepository.useOnlineInMemoryPickListState) onlineInMemoryPickData else OnlineInMemoryPickData(),
                        offlinePickData = offlinePickData ?: OfflinePickData(),
                        upcToItemIdMap = upcToItemIdMap
                    )
                )
                _picklist.value = unifiedPickListState
                Timber.v("[updatePickListState] pick list updated")
                /**
                 * To stop timer itemActivities won't be null.
                 * In case of assignUser api call itemActivities will be null.
                 * To avoid calling stopTimerIfRequired method if picklist does not contains itemActivities.
                 */
                _picklist.value?.itemActivities.takeIf { it.isNotNullOrEmpty() }?.let { stopTimerIfRequired() }
            }
        } ?: run { _picklist.value = null }
    }

    override suspend fun modifyPickList(item: ItemActivityDto) {
        val modify = _picklist.value?.itemActivities?.map {
            if (it.id == item.id) {
                it.copy(isPickCompleted = true)
            } else
                it
        }
        val list = _picklist.value?.copy(itemActivities = modify)
        delay(500)
        _picklist.value = list
    }

    /** Reads offline/cached data from disk and initializes [baselineActivityDetails] and [itemUpcs] */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun readFromDisk() {
        withContext(dispatcherProvider.IO) {
            offlineDataMutex.withLock {
                if (offlinePickFile.exists()) {
                    try {
                        val adapter: JsonAdapter<OfflinePickData> = moshi.adapter(OfflinePickData::class.java)
                        offlinePickData = adapter.fromJson(offlinePickFile.source().buffer())
                        baselineActivityDetails = offlinePickData?.baselineActivityDetails
                        itemUpcs.clear()
                        itemUpcs.addAll(offlinePickData?.itemUpcs.orEmpty())
                        generateUpcToItemIdMap()
                        substitutionItemDetailList.clear()
                        substitutionItemDetailList.addAll(offlinePickData?.substitutionItemDetails.orEmpty())
                        setActivePickListActivityId(offlinePickData?.baselineActivityDetails?.actId?.toString() ?: "")
                        Timber.v("[readFromDisk] offlinePickData=$offlinePickData")
                    } catch (e: IOException) {
                        Timber.w(e, "[readFromDisk] error reading offline file")
                    }
                }
            }
        }
    }

    /** Writes offline/cached data to disk and updates [offlinePickData] */
    // TODO: Determine if something needs to be done to prevent writing while another write is taking place (some coroutine sentinel or similar)?
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun writeToDisk(offlinePickDataArg: OfflinePickData?) {
        withContext(dispatcherProvider.IO) {
            offlineDataMutex.withLock {
                offlinePickData = pickListProcessor.optimizeOfflineData(offlinePickDataArg)
                val adapter: JsonAdapter<OfflinePickData> = moshi.adapter(OfflinePickData::class.java)
                // https://stackoverflow.com/questions/26969800/try-with-resources-in-kotlin
                offlinePickFile.sink().buffer().use { sink -> adapter.toJson(sink, offlinePickData) }
            }
        }
    }
    /** Delegates to [wrapExceptions], passing in the class name here instead of requiring it of all callers */
    private suspend fun <T : Any> wrapExceptions(methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        return wrapExceptions("PickRepository", methodName, block)
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    private fun <T : Any> Response<T>.toEmptyResult(): ApiResult<Unit> {
        return responseToApiResultMapper.toEmptyResult(this)
    }
}

/** Appends the primaryUpc for matching item in [itemActivities] to the [ItemUpcDto.upcList]. No change to upcList if primary upc is null or blank. internal visibility for easier testing */
@SuppressLint("BinaryOperationInTimber")
internal fun List<ItemUpcDto>.appendPrimaryUpcToItemUpcList(itemActivities: List<ItemActivityDto>): List<ItemUpcDto> {
    // Add any item in itemActivities to itemUpcDto list that isn't already there
    // This is a workaround for the scenario outlined in ACUPICK-1268, which is /api/itemUpcList not returning entries for some or all of the items in the pick list
    val updatedItemUpcList = this.toMutableList().apply {
        addAll(
            itemActivities.filter { itemActivity ->
                this.map { it.itemId }.contains(itemActivity.itemId).not() &&
                    itemActivity.primaryUpc.isNotNullOrEmpty()
            }.map { ItemUpcDto(it.itemId, emptyList()) }
        )
    }
    return updatedItemUpcList.map { itemUpcDto ->
        // Since multiple identical items can exist in batch orders and technically contain different primaryUpc values (although not expected), gather the unique set of all primaryUpcs.
        val matchingPrimaryUpcs: Set<String> = itemActivities.filter { it.itemId == itemUpcDto.itemId }.map { it.primaryUpc.orEmpty() }.filter { it.isNotEmpty() }.toSet()
        if (matchingPrimaryUpcs.isNotEmpty()) {

            itemUpcDto.copy(upcList = itemUpcDto.upcList?.toMutableList()?.plus(matchingPrimaryUpcs)?.distinct()).also {
                val originalCount = itemUpcDto.upcList.orEmpty().count()
                val updatedCount = it.upcList.orEmpty().count()
                if (originalCount != updatedCount) {
                    // TODO: Log this scenario in analytics to provide data on how widespread this issue to the team!
                    Timber.w(
                        "[appendPrimaryUpcToItemUpcList] the primary upcs '${matchingPrimaryUpcs.joinToString()}' for item id (BPN) '${itemUpcDto.itemId}' were missing" +
                            " from the itemUpcList backend response - added by the frontend"
                    )
                }
            }
        } else {
            itemUpcDto
        }
    }
}

private val SYNC_NETWORK_TIMEOUT_FAILURE_NEXT_SYNC_DELAY_DURATION: Duration = Duration.ofSeconds(5)
private const val SECONDS = 60

/**
 * Wrap suspend fun code that you want to test for proper thread handling in as a lambda to this function to put it through its paces. Likely move to unit tests
 *
 * Inspired by https://kotlinlang.org/docs/reference/coroutines/shared-mutable-state-and-concurrency.html#the-problem
 *
 * Example:
 *
 * ```
 * GlobalScope.launch(dispatcherProvider.IO) {
 *     massiveRun {
 *         readFromDisk()
 *         writeToDisk(offlinePickData!!.copy(itemUpcs = listOf(ItemUpcDto(upcList = listOf(UUID.randomUUID().toString())))))
 *     }
 * }
 * ```
 */
private suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100 // number of coroutines to launch
    val k = 10 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}

/**
 * Countdown timer will not be start if the short item has [PREP_NOT_READY_VALUE] and [TOTE_FULL_VALUE]
 */
private fun ShortReasonCode?.shouldStartCountDownTimer() = this != ShortReasonCode.PREP_NOT_READY && this != ShortReasonCode.TOTE_FULL

/**
 * Master-order-view: Validating swap substitutio rejection code
 * Will use newIaId only if reason code is [SubstitutionRejectedReason.SWAP_OTHER_PICKLIST] or [SubstitutionRejectedReason.SWAP_OOS_OTHER_PICKLIST]
 */
private fun SubstitutionRejectedReason?.shoulUseOtherPickersIaId(): Boolean = this == SubstitutionRejectedReason.SWAP_OTHER_PICKLIST || this == SubstitutionRejectedReason.SWAP_OOS_OTHER_PICKLIST
