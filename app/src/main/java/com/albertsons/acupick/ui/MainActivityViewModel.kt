package com.albertsons.acupick.ui

import android.app.Application
import android.graphics.drawable.Drawable
import android.os.Environment
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.albertsons.acupick.AcuPickMessagingService
import com.albertsons.acupick.R
import com.albertsons.acupick.data.buildconfig.BuildConfigProvider
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.fullContactName
import com.albertsons.acupick.data.model.request.ChatErrorData
import com.albertsons.acupick.data.model.request.ErrorData
import com.albertsons.acupick.data.model.request.ErrorMessage
import com.albertsons.acupick.data.model.request.Event
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
import com.albertsons.acupick.data.model.response.AcknowledgedPickerDetailsDto
import com.albertsons.acupick.data.model.response.isAuthenticationError
import com.albertsons.acupick.data.model.response.isBatchOrder
import com.albertsons.acupick.data.network.NetworkAvailabilityController
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ArrivalsRepository
import com.albertsons.acupick.data.repository.ConversationsClientWrapper
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.LoginLogoutAnalyticsRepository
import com.albertsons.acupick.data.repository.MessagesRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.StagingStateRepository
import com.albertsons.acupick.data.repository.TokenizedLdapRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.combineOncePerChange
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.arrivals.TimerHeaderData
import com.albertsons.acupick.ui.bottomsheetdialog.CustomBottomSheetArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.SnackBarEvent
import com.albertsons.acupick.ui.util.AnalyticsHelper
import com.albertsons.acupick.ui.util.AnalyticsHelper.ErrorType
import com.albertsons.acupick.ui.util.AnalyticsHelper.NetworkErrorType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.firstAndLastName
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.storeNumberTitleForNavMenuItem
import com.hadilq.liveevent.LiveEvent
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class MainActivityViewModel(
    val app: Application,
    private val userRepo: UserRepository,
    private val barcodeMapper: BarcodeMapper,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val networkAvailabilityController: NetworkAvailabilityController,
    private val moshi: Moshi,
    buildConfigProvider: BuildConfigProvider,
    private val dispatcherProvider: DispatcherProvider,
) : BaseViewModel(app) {

    // DI
    private val analyticsHelper: AnalyticsHelper by inject()
    private val stagingStateRepo: StagingStateRepository by inject()
    private val idRepository: IdRepository by inject()
    private val apsRepository: ApsRepository by inject()
    private val sitesRepository: SiteRepository by inject()
    private val wineShippingStageStateRepository: WineShippingStageStateRepository by inject()
    private val loginLogoutAnalyticsRepo: LoginLogoutAnalyticsRepository by inject()
    private val arrivalsRepo: ArrivalsRepository by inject()
    private val chatRepository: ConversationsRepository by inject()
    private val messagesRepository: MessagesRepository by inject()
    private val pickRepository: PickRepository by inject()
    private val conversationsClient: ConversationsClientWrapper by inject()
    private val tokenizedLdapRepository: TokenizedLdapRepository by inject()

    // data
    val devOptionsEnabled = buildConfigProvider.isDebugOrInternalBuild
    val toolbarTitle: LiveData<String> = MutableLiveData()
    val toolbarTitleBackground: LiveData<Drawable> = MutableLiveData()
    val toolbarSmallTitle: LiveData<String> = MutableLiveData()
    val toolbarLeftExtra: LiveData<String> = MutableLiveData()
    val toolbarLeftExtraImage: LiveData<Drawable> = MutableLiveData()
    val toolbarRightExtraTop: LiveData<String> = MutableLiveData()
    val toolbarRightExtraBottom: LiveData<String> = MutableLiveData()
    val toolbarNavigationIcon: LiveData<Drawable> = MutableLiveData()
    val toolbarExtraRight: LiveData<String> = MutableLiveData()
    val toolbarExtraRightCta: LiveData<String?> = MutableLiveData(null)
    val toolbarExtraRightCtaVisibility: LiveData<Int> = toolbarExtraRightCta.map { if (it.isNullOrEmpty()) View.GONE else View.VISIBLE }
    val toolbarRightExtraSecondImage: LiveData<Drawable> = MutableLiveData()
    val toolbarRightExtraFirstImage: LiveData<Drawable> = MutableLiveData()
    val displayName: LiveData<String> = MutableLiveData()
    val toolBarVisibility: LiveData<Boolean> = MutableLiveData()

    // ui
    val isLoading: LiveData<Boolean> = MutableLiveData()
    val blockUi: LiveData<Boolean> = MutableLiveData()
    val hasSingleSiteAssigned: LiveData<Boolean> = MutableLiveData(false)
    val keyboardActive = MutableLiveData<Boolean>()
    val isStepProgressViewVisible: LiveData<Boolean> = MutableLiveData()
    val progressTimer: LiveData<TimerHeaderData?> = MutableLiveData()
    // To identify if app resumes due to notification/interjection or due to a normal flow. This is needed as in some screens we make api calls in onResume() method which is not needed in
    // notification flow. Why live data is not used here? - Because onResume() method is getting called before live data observes the value(isAppResumesFromNotifiction), but we need the value before
    // onResume to block unnecessary api calls.
    // TODO need to find a better way to identify it.
    private var isAppResumesFromNotifiction = false

    // state
    private var toolbarRightExtraClickBlock: (() -> Unit)? = null

    // callback to be provided to extension function for keyboard changes
    val keyboardCallback: (visible: Boolean) -> Unit = { keyboardActive.value = it }

    // events
    val triggerOfflineError: LiveData<Unit> = LiveEvent<Unit>()
    val scannedData: LiveData<BarcodeType> = LiveEvent<BarcodeType>()
    val userLoggedIn: LiveData<Boolean> = LiveEvent<Boolean>()
    val navigationButtonIntercept: LiveData<Unit> = LiveEvent()
    val toolbarRightSecondImageClickEvent: LiveData<Unit> = LiveEvent()
    val toolbarRightFirstImageClickEvent: LiveData<Unit> = LiveEvent()
    val triggerHomeClickIntercept: LiveData<Unit> = LiveEvent()
    val snackBarEvent = LiveEvent<SnackBarEvent<Long>>()
    val snackEvent = LiveEvent<AcupickSnackEvent>()
    val activityDialogEvent = LiveEvent<CustomDialogArgDataAndTag>()
    val activityNavigationEvent: LiveEvent<NavDirections> = LiveEvent()
    val emptyToolbarEvent: LiveEvent<Unit> = LiveEvent()
    var retryAction: (() -> Unit)? = null
    val bottomSheetRecordPickArgData: MutableLiveData<CustomBottomSheetArgData> = LiveEvent() // This variable is used to post data on active bottomsheet
    val bottomSheetBackButtonPressed: MutableLiveData<Unit> = LiveEvent() // This variable is used to post data on active fragment when back button of bottom sheet pressed
    val storeTitle: LiveData<String> = MutableLiveData()
    val acknowledgedPickerDetails: LiveData<AcknowledgedPickerDetailsDto> = LiveEvent()
    val updateTimerTime: LiveData<String?> = MutableLiveData()
    val is1Pl: LiveData<Boolean> = MutableLiveData(false)
    val orderNumberFor1PlToSelect = MutableLiveData<Long>()

    val blockStagingHandleEvent: MutableStateFlow<Boolean> = MutableStateFlow(false)
    fun switchArrivalType() = is1Pl.value.orFalse().not().also { is1Pl.postValue(it) }
    var isRunning = false
    init {

        viewModelScope.launch(dispatcherProvider.IO) {
            userRepo.user.collect { user ->
                if (user == null) {
                    displayName.postValue("")
                } else {
                    displayName.postValue(user.firstAndLastName())
                    hasSingleSiteAssigned.postValue(user.sites.size == 1)
                    storeTitle.postValue(user.storeNumberTitleForNavMenuItem())
                }
            }
        }
        // Note that you currently can't place two flows in a single viewModelScope.launch block which is why they are separated into two blocks
        viewModelScope.launch(dispatcherProvider.IO) {
            networkAvailabilityController.showOfflineError.filterNotNull().collect {
                triggerOfflineError.postValue(Unit)
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            chatRepository.newMessageNotification.collect {
                Timber.v("newMessageNotification $it")
                it?.let { message ->
                    val orderChatDetail = pickRepository.pickList.value?.orderChatDetails
                        ?.firstOrNull { orderChatDetail ->
                            orderChatDetail.conversationSid?.let { conversationSid -> conversationSid == message.conversationSid }
                                ?: (chatRepository.getConversationId(orderChatDetail.customerOrderNumber.orEmpty()) == message.conversationSid)
                        }
                    orderChatDetail?.let { detail ->
                        AcuPickMessagingService(app).createNotification(
                            mapOf(
                                "notificationType" to "CHAT",
                                "customerName" to detail.fullContactName().orEmpty(),
                                "customerMessage" to message.body.orEmpty(),
                                "orderNumber" to detail.customerOrderNumber.orEmpty()
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            chatRepository.pickerJoinedNotification.collect {
                Timber.v("pickerJoinedNotification $it")
                it?.let { message ->
                    val orderChatDetail = pickRepository.pickList.value?.orderChatDetails
                        ?.firstOrNull { orderChatDetail ->
                            orderChatDetail.conversationSid?.let { conversationSid -> conversationSid == message.conversationSid }
                                ?: (chatRepository.getConversationId(orderChatDetail.customerOrderNumber.orEmpty()) == message.conversationSid)
                        }
                    orderChatDetail?.let { detail ->
                        AcuPickMessagingService(app).createNotification(
                            mapOf(
                                "notificationType" to "CHAT_PICKER",
                                "pickerJoined" to "true",
                                "pickerName" to tokenizedLdapRepository.pickersLdapDetails.value.getOrDefault(it.attributes?.tokenizedLdapId, ""),
                                "orderNumber" to detail.customerOrderNumber.orEmpty()
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            chatRepository.pickerLeftNotification.collect {
                Timber.v("pickerLeftNotification $it")
                it?.let { message ->
                    val orderChatDetail = pickRepository.pickList.value?.orderChatDetails
                        ?.firstOrNull { orderChatDetail ->
                            orderChatDetail.conversationSid?.let { conversationSid -> conversationSid == message.conversationSid }
                                ?: (chatRepository.getConversationId(orderChatDetail.customerOrderNumber.orEmpty()) == message.conversationSid)
                        }
                    orderChatDetail?.let { detail ->
                        AcuPickMessagingService(app).createNotification(
                            mapOf(
                                "notificationType" to "CHAT_PICKER",
                                "pickerJoined" to "false",
                                "pickerName" to tokenizedLdapRepository.pickersLdapDetails.value.getOrDefault(it.attributes?.tokenizedLdapId, ""),
                                "orderNumber" to detail.customerOrderNumber.orEmpty()
                            )
                        )
                    }
                }
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            chatRepository.initializationError.collect {
                it?.let {
                    Timber.d("initializationError $it")
                    val shouldRetry = (
                        (it.second as? ApiResult.Failure.GeneralFailure)?.networkCallName == NetworkCalls.TWILIO_SDK_INITIALIZATION_FAILURE.value ||
                            (it.second as? ApiResult.Failure.GeneralFailure)?.networkCallName == NetworkCalls.GET_TWILIO_TOKEN_FAILURE.value
                        ) && isRunning.not()
                    if (shouldRetry) {
                        startSdkRetries()
                    } else if ((it.second as? ApiResult.Failure.GeneralFailure)?.networkCallName == NetworkCalls.SUCCESS_SUBSCRIPTION_TO_CHAT.value) {
                        fetchChat()
                    }
                    handleTwilioError(it)
                }
            }
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            Timber.d("combineOncePerChange: other.collect")
            // onPickerAdded gets called before pickRepository.pickList is updated resulting in a null empty orderNumber
            // this funtion uses the pickRepository.pickList whenever it is updated to send the chat message
            chatRepository.onPickerAdded
                .combineOncePerChange(pickRepository.pickList) { conversationSid, picklist ->
                    Timber.d("onPickerAdded $conversationSid")
                    conversationSid?.let { sid ->
                        val user = userRepo.user.value
                        val orderNumber = picklist.orderChatDetails
                            ?.firstOrNull { it.conversationSid == conversationSid }
                            ?.customerOrderNumber
                            ?: chatRepository.getOrderIdByConversationSid(conversationSid)
                        if (user != null) {
                            messagesRepository.sendPickerJoinedMessage(sid, orderNumber, user.userId, user.tokenizedLdapId.orEmpty())
                        }
                    }
                }.collect()
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            userRepo.isLoggedIn.collect { isLoggedIn ->
                Timber.v(
                    "[init] user %s".format(
                        when (isLoggedIn) {
                            true -> "logged in"
                            false -> "just logged out"
                        }
                    )
                )
                userLoggedIn.postValue(isLoggedIn)
            }
        }
    }

    private fun fetchChat() {
        viewModelScope.launch {
            pickRepository.pickList.value?.orderChatDetails?.map {
                async {
                    val sid = chatRepository.getConversationId(it.customerOrderNumber.orEmpty())
                    if (sid.isNotNullOrEmpty()) {
                        chatRepository.insertOrUpdateConversation(sid)
                    }
                }
            }?.awaitAll() ?: run { chatRepository.clear() }
        }
    }

    /**
     * The method uses a coroutine to run the retry logic in the background.
     * It first checks if the Twilio SDK client is not created, if the total number of retry intervals is less than the maximum allowed, and if the user is logged in.
     * The method then enters a loop where it retries the initialization of the Twilio SDK.
     * It uses a delay between retries, which is specified by the chatRetryDelaySecond value from the sitesRepository.
     * The method also keeps track of the number of retries (retries) and the total number of retry intervals (totalIntervals).
     * If the number of retries reaches the maximum allowed within a certain interval (chatMaxRetriesWithinInterval), it waits until the next interval before retrying again.
     * The total number of retry intervals is limited by the chatTotalRetryIntervals value from the sitesRepository.
     * If the total number of intervals reaches this limit, the method stops retrying.
     */
    private fun startSdkRetries() {
        viewModelScope.launch(dispatcherProvider.IO) {
            userRepo.user.value?.userId?.let {
                val retryDelay = sitesRepository.twoWayCommsFlags.chatRetryDelaySecond ?: 0
                val chatRetryTimeIntervalSeconds = sitesRepository.twoWayCommsFlags.chatRetryTimeIntervalSeconds ?: 0
                val chatMaxRetriesWithinInterval = sitesRepository.twoWayCommsFlags.chatMaxRetriesWithinInterval ?: 0
                val chatTotalRetryIntervals = sitesRepository.twoWayCommsFlags.chatTotalRetryIntervals ?: 0

                var retries = 0
                var totalIntervals = 0
                var startTime = System.currentTimeMillis()
                Timber.d("startSdkRetries: ${conversationsClient.isClientCreated.not() && totalIntervals < chatTotalRetryIntervals && userRepo.isLoggedIn.value}")

                while (conversationsClient.isClientCreated.not() && totalIntervals < chatTotalRetryIntervals && userRepo.isLoggedIn.value) {
                    isRunning = true
                    val currentTime = System.currentTimeMillis()
                    Timber.d("startSdkRetries: outside ${(chatRetryTimeIntervalSeconds - ((currentTime - startTime) / 1000))}")

                    if ((chatRetryTimeIntervalSeconds - ((currentTime - startTime) / 1000)) <= 0) {
                        Timber.d("startSdkRetries: inside $retries")
                        retries = 0
                        startTime = currentTime
                        totalIntervals++
                    }

                    if (retries < chatMaxRetriesWithinInterval && totalIntervals < chatTotalRetryIntervals) {
                        retries++
                        chatRepository.initializeChat(it, false)
                        Timber.d("startSdkRetries: $retries, -- $retryDelay")
                        delay(retryDelay * 1000)
                    } else {
                        Timber.d("startSdkRetries: else ${chatRetryTimeIntervalSeconds - (currentTime - startTime) / 1000}")
                        // if total retries are coompleted in a interval then delay until next interval
                        delay((chatRetryTimeIntervalSeconds * 1000) - (currentTime - startTime))
                    }
                }
                isRunning = false
            }
        }
    }

    fun onNavigationIntercepted() {
        navigationButtonIntercept.postValue(Unit)
    }

    fun setToolBarVisibility(visible: Boolean) {
        toolBarVisibility.postValue(visible)
    }

    fun updateNetworkStatus(connected: Boolean) {
        viewModelScope.launch(dispatcherProvider.IO) {
            networkAvailabilityController.updateNetworkStatus(connected)
        }
    }

    fun retryNetworkRequest() {
        viewModelScope.launch(dispatcherProvider.IO) {
            networkAvailabilityManager.tryAgainLambda?.invoke()
        }
    }

    @JvmOverloads
    fun manualLogout() {
        viewModelScope.launch(dispatcherProvider.IO) {
            loginLogoutAnalyticsRepo.sendOrSaveLogoutData(UserActivityRequestDto.ACTIVITY_LOGOUT_REASON_USER_INITIATED)
            logout()
        }
    }

    fun logout() {
        viewModelScope.launch(dispatcherProvider.IO) {
            userRepo.logout()
            stagingStateRepo.clear()
            wineShippingStageStateRepository.clear()
            idRepository.clear()
            arrivalsRepo.clear()
            clearPictureDir()
            is1Pl.postValue(false)
        }
    }

    private fun clearPictureDir() {
        viewModelScope.launch(Dispatchers.IO) {
            val picturesDir = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            picturesDir?.let {
                it.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        }
    }

    fun setScannedData(dataScanned: String?) {
        val barcodeType = barcodeMapper.inferBarcodeType(dataScanned.orEmpty(), enableLogging = true)
        Timber.v("[setScannedData] barcodeType=$barcodeType")
        scannedData.postValue(barcodeType)
    }

    fun setLoadingState(isLoading: Boolean, blockUi: Boolean = false) {
        this.isLoading.postValue(isLoading)
        this.blockUi.postValue(blockUi && isLoading)
    }

    fun setAppResumesFromNotification(isFromNotification: Boolean = false) {
        isAppResumesFromNotifiction = isFromNotification
    }

    fun getIsAppResumesFromNotification(): Boolean {
        return isAppResumesFromNotifiction
    }

    fun setToolbarRightExtra(value: String = "") {
        toolbarExtraRight.postValue(value)
    }

    /** Adds a clickable cta on the right side of the toolbar */
    fun setToolbarRightExtraCta(value: String = "", clickBlock: () -> Unit) {
        toolbarExtraRightCta.postValue(value)
        toolbarRightExtraClickBlock = clickBlock
    }

    fun onToolbarRightExtraClick() {
        toolbarRightExtraClickBlock?.invoke()
    }

    fun setToolbarTitle(value: String = "") {
        toolbarTitle.postValue(value)
    }

    fun setToolbarTitleBackground(value: Drawable?) {
        toolbarTitleBackground.postValue(value)
    }

    fun setToolbarSmallTitle(value: String = "") {
        toolbarSmallTitle.postValue(value)

    }

    fun setToolbarLeftExtra(value: String = "") {
        toolbarLeftExtra.postValue(value)
    }

    fun setToolbarLeftExtraImage(value: Drawable? = null) {
        value?.let { toolbarLeftExtraImage.postValue(value) }
    }

    fun setToolbarRightSecondExtraImage(value: Drawable? = null) {
        value?.let { toolbarRightExtraSecondImage.postValue(value) }
    }

    fun setToolbarRightFirstExtraImage(value: Drawable? = null) {
        value?.let { toolbarRightExtraFirstImage.postValue(value) }
    }

    fun onToolbarRightSecondImageClick() {
        toolbarRightSecondImageClickEvent.postValue(Unit)
    }

    fun onToolbarRightFirstImageClick() {
        toolbarRightFirstImageClickEvent.postValue(Unit)
    }

    fun triggerHomeButtonEvent() {
        triggerHomeClickIntercept.postValue(Unit)
    }

    fun setToolbarRightExtraTop(value: String = "") {
        toolbarRightExtraTop.postValue(value)
    }

    fun setToolbarRightExtraBottom(value: String = "") {
        toolbarRightExtraBottom.postValue(value)
    }

    fun setToolbarNavigationIcon(value: Drawable? = null) {
        toolbarNavigationIcon.postValue(value)
    }

    fun emptyToolbar() {
        emptyToolbarEvent.postValue(Unit)
    }

    fun setAcknowledgedPickerDetails(details: AcknowledgedPickerDetailsDto?) {
        acknowledgedPickerDetails.postValue(details)
    }

    fun setToolBarTimer(data:TimerHeaderData?){
        progressTimer.postValue(data)
        data?.let {
            val timer = app.getString(R.string.wait_time_countdown, it.elapsedTime.div(60), it.elapsedTime.rem(60))
            Timber.e("Timer -> Long ${it.elapsedTime}")
            Timber.e("Timer -> Minutes ${it.elapsedTime.div(60)}")
            Timber.e("Timer -> Second ${ it.elapsedTime.rem(60)}")
            updateTimerTime.postValue(timer)
            isStepProgressViewVisible.postValue(true)
        } ?: run {
            isStepProgressViewVisible.postValue(false)
        }
    }

    val clearSnackBar: ((SnackBarEvent<Long>?) -> Unit) = {
        if (snackBarEvent.value == it) snackBarEvent.postValue(null)
    }

    fun clearToolbar() {
        toolbarTitle.postValue("")
        toolbarTitleBackground.postValue(null)
        toolbarSmallTitle.postValue("")
        toolbarLeftExtra.postValue("")
        toolbarRightExtraBottom.postValue("")
        toolbarRightExtraTop.postValue("")
        toolbarExtraRight.postValue("")
        toolbarExtraRightCta.postValue("")
        toolbarLeftExtraImage.postValue(null)
        toolbarRightExtraSecondImage.postValue(null)
        toolbarRightExtraFirstImage.postValue(null)
        toolbarRightExtraClickBlock = null
        isStepProgressViewVisible.postValue(false)
        updateTimerTime.postValue(null)
        progressTimer.postValue(null)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Push Notification related functions
    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////
    // Error Handling functions
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This function should only be called from BaseFragment or other fragment code.
     *
     * BaseViewModel has a handleApiError function that connects to this without explicit dependencies.
     */
    suspend fun handleApiErrors(errorType: ApiResult.Failure, tag: String = ERROR_DIALOG_TAG, retryActionEvent: (() -> Unit)? = null) {

        retryAction = retryActionEvent

        // Note that we don't want to show a modal for an auth error as the only time you should see an auth error is when a refreshToken call
        // has failed to retrieve a new token and the picker is in the process of being logged out.
        val isAuthenticationError = (errorType as? ApiResult.Failure.Server)?.error?.isAuthenticationError() == true

        when (networkAvailabilityManager.isConnected.first() && !isAuthenticationError) {
            true -> {
                when (errorType) {
                    is ApiResult.Failure.Server -> {
                        val errorMessage = errorType.error?.message
                        val httpErrorCode = errorType.error?.httpErrorCode
                        val serverErrorCode = errorType.error?.errorCode?.rawValue
                        showServerErrorDialog(
                            tag = tag,
                            httpErrorCode?.toString(),
                            serverErrorCode?.toString(),
                            errorMessage
                        )
                        analyticsHelper.sendErrorEvent(
                            eventSource = tag,
                            errorType = ErrorType.SERVER_ERROR,
                            errorMessage = errorMessage,
                            httpErrorCode = httpErrorCode,
                            serverErrorCode = serverErrorCode
                        )
                    }
                    is ApiResult.Failure.NetworkFailure -> {
                        val eventMessage = when (errorType) {
                            is ApiResult.Failure.NetworkFailure.Timeout -> NetworkErrorType.TIMEOUT_ERROR
                            is ApiResult.Failure.NetworkFailure.VpnError -> NetworkErrorType.VPN_ERROR
                        }
                        showNetworkErrorDialog(errorType)
                        analyticsHelper.sendErrorEvent(
                            eventSource = NETWORK_ERROR_DIALOG_TAG,
                            errorType = ErrorType.NETWORK_ERROR,
                            errorMessage = eventMessage
                        )
                    }
                    else -> {
                        showGenericErrorDialog()
                        analyticsHelper.sendErrorEvent(
                            eventSource = RETRY_ERROR_DIALOG_TAG,
                            errorType = ErrorType.GENERIC_ERROR,
                        )
                    }
                }
            }
            false -> {
                // no-op if not connected or if an auth error
                if (isAuthenticationError) {
                    val httpErrorCode = (errorType as? ApiResult.Failure.Server)?.error?.httpErrorCode
                    val errorMessage = (errorType as? ApiResult.Failure.Server)?.error?.message
                    analyticsHelper.sendErrorEvent(
                        eventSource = tag,
                        errorType = ErrorType.AUTH_ERROR,
                        errorMessage = errorMessage,
                        httpErrorCode = httpErrorCode
                    )
                } else {
                    analyticsHelper.sendErrorEvent(
                        eventSource = tag,
                        errorType = ErrorType.NETWORK_ERROR,
                        errorMessage = NetworkErrorType.NOT_CONNECTED_ERROR
                    )
                }
            }
        }
    }

    private fun handleTwilioError(error: Pair<Boolean, ApiResult.Failure>) {
        when (val errorType = error.second) {
            is ApiResult.Failure.Server -> {
                val errorMessage = errorType.error?.message
                val httpErrorCode = errorType.error?.httpErrorCode
                val serverErrorCode = errorType.error?.errorCode?.rawValue
                acuPickLogger.e(
                    "eventSource = ${errorType.networkCallName}, " +
                        "errorType = ${ErrorType.SERVER_ERROR}," +
                        "errorMessage = $errorMessage," +
                        "httpErrorCode = $httpErrorCode, " +
                        "serverErrorCode = $serverErrorCode"
                )
                viewModelScope.launch {
                    apsRepository.logError(
                        ErrorMessage(
                            event = Event(errorType.networkCallName, errorType.error.toString(), ZonedDateTime.now()),
                            data = ErrorData(
                                userRepo.user.value?.selectedStoreId,
                                userRepo.user.value?.userId
                            )
                        )
                    )
                }
                if (error.first) {
                    showTwilioErrorDialog(
                        tag = TWILIO_ERROR_DIALOG_TAG,
                        app.resources.getString(R.string.something_wrong_body_with_cause_format, "$errorType")
                    )
                }
            }
            is ApiResult.Failure.NetworkFailure -> {
                val eventMessage = when (errorType) {
                    is ApiResult.Failure.NetworkFailure.Timeout -> NetworkErrorType.TIMEOUT_ERROR
                    is ApiResult.Failure.NetworkFailure.VpnError -> NetworkErrorType.VPN_ERROR
                }
                acuPickLogger.e("eventSource = ${errorType.networkCallName}, errorType = ${ErrorType.NETWORK_ERROR}, errorMessage = $eventMessage")
                if (error.first) {
                    showTwilioErrorDialog(
                        tag = TWILIO_ERROR_DIALOG_TAG,
                        app.resources.getString(R.string.something_wrong_body_with_cause_format, "$errorType")
                    )
                }
            }
            is ApiResult.Failure.GeneralFailure -> {
                acuPickLogger.e("eventSource = ${errorType.networkCallName}, errorType = $errorType")
                val isBatchOrder = pickRepository.pickList.value?.isBatchOrder().orFalse()
                val errorData = runCatching { moshi.adapter(ChatErrorData::class.java).fromJson(errorType.message) }.getOrNull()
                viewModelScope.launch {
                    apsRepository.logError(
                        ErrorMessage(
                            event = Event(errorType.networkCallName, errorData?.errorMessage ?: "", ZonedDateTime.now()),
                            data = ErrorData(
                                storeNumber = userRepo.user.value?.selectedStoreId,
                                userId = userRepo.user.value?.userId,
                                orderNumber = errorData?.orderNumbers?.firstOrNull().takeUnless { isBatchOrder },
                                conversationSid = errorData?.conversationSids?.firstOrNull().takeUnless { isBatchOrder },
                                stagingBlockedTime = errorData?.stagingBlockedTime,
                                orderNumbers = errorData?.orderNumbers.takeIf { isBatchOrder },
                                conversationSids = errorData?.conversationSids.takeIf { isBatchOrder }
                            )
                        )
                    )
                }
                if (error.first) {
                    showTwilioErrorDialog(
                        tag = TWILIO_ERROR_DIALOG_TAG,
                        app.resources.getString(R.string.something_wrong_body_with_cause_format, "$errorType")
                    )
                }
            }
        }
    }

    /** Shows an error dialog with http code, server code, and server message. Not intended to support retry. */
    private fun showServerErrorDialog(tag: String, httpErrorCode: String?, serverErrorCode: String?, serverErrorMessage: String?) {
        val secondaryBody = app.resources.getString(R.string.something_wrong_body_with_type_api_error).plus("\n")
            .plus(app.resources.getString(R.string.something_wrong_body_with_source_backend)).plus("\n")
            .plus(app.resources.getString(R.string.something_wrong_body_with_http_code_format, httpErrorCode)).plus("\n")
            .plus(app.resources.getString(R.string.something_wrong_body_with_server_code_format, serverErrorCode)).plus("\n")
            .plus(app.resources.getString(R.string.something_wrong_body_with_server_message_format, serverErrorMessage)).plus("\n")

        val args = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.something_went_wrong),
            body = StringIdHelper.Id(R.string.something_wrong_body),
            secondaryBody = StringIdHelper.Raw(secondaryBody),
            positiveButtonText = StringIdHelper.Id(R.string.ok),
            cancelOnTouchOutside = false
        )
        activityDialogEvent.postValue(CustomDialogArgDataAndTag(data = args, tag = tag))
    }

    private fun showTwilioErrorDialog(tag: String, secondaryBody: String) {
        val args = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.chat_initialization_failed),
            body = StringIdHelper.Id(R.string.chat_initialization_failed_body),
            secondaryBody = StringIdHelper.Raw(secondaryBody),
            positiveButtonText = StringIdHelper.Id(R.string.app_restart_button),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false
        )
        activityDialogEvent.postValue(CustomDialogArgDataAndTag(data = args, tag = tag))
    }

    /** Shows an error dialog with network timeout or vpn issues. Intended to support retry. */
    private fun showNetworkErrorDialog(networkFailure: ApiResult.Failure.NetworkFailure) {
        val secondaryBody = app.resources.getString(R.string.something_wrong_body_with_type_network_error).plus("\n")
            .plus(app.resources.getString(R.string.something_wrong_body_with_source_device)).plus("\n")
            // exception.toString will print something like the following that includes the exception type and message: java.net.SocketTimeoutException: SSL handshake timed out
            .plus(app.resources.getString(R.string.something_wrong_body_with_cause_format, networkFailure.exception.toString())).plus("\n")
            .plus(
                when (networkFailure) {
                    is ApiResult.Failure.NetworkFailure.Timeout -> app.resources.getString(R.string.something_wrong_body_with_additional_info_timeout_error).plus("\n")
                    is ApiResult.Failure.NetworkFailure.VpnError -> app.resources.getString(R.string.something_wrong_body_with_additional_info_vpn_error).plus("\n")
                }
            )

        val args = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.something_went_wrong),
            body = StringIdHelper.Id(R.string.something_wrong_body),
            secondaryBody = StringIdHelper.Raw(secondaryBody),
            positiveButtonText = StringIdHelper.Id(R.string.try_again),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false,
        )
        activityDialogEvent.postValue(CustomDialogArgDataAndTag(data = args, tag = RETRY_ERROR_DIALOG_TAG))
    }

    /** Shows an error dialog with no information*/
    private fun showGenericErrorDialog() {
        val args = CustomDialogArgData(
            titleIcon = R.drawable.ic_alert,
            title = StringIdHelper.Id(R.string.something_went_wrong),
            body = StringIdHelper.Id(R.string.something_wrong_body),
            positiveButtonText = StringIdHelper.Id(R.string.try_again),
            negativeButtonText = StringIdHelper.Id(R.string.cancel),
            cancelOnTouchOutside = false
        )
        activityDialogEvent.postValue(CustomDialogArgDataAndTag(data = args, tag = RETRY_ERROR_DIALOG_TAG))
    }


    companion object {
        const val ERROR_DIALOG_TAG = "errorDialogTag"
        const val TWILIO_ERROR_DIALOG_TAG = "twilioerrorDialogTag"
        const val NETWORK_ERROR_DIALOG_TAG = "networkErrorDialogTag"
        const val RETRY_ERROR_DIALOG_TAG = "genericErrorDialogTag"
    }
}
