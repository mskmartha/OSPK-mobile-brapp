package com.albertsons.acupick.ui.notification

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.duginterjection.failureReasonTextValue
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.notification.NotificationData
import com.albertsons.acupick.data.model.notification.NotificationType
import com.albertsons.acupick.data.model.notification.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.notification.getNotificationReceivedCount
import com.albertsons.acupick.data.model.notification.isFinalNotification
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.NotificationRequestDto
import com.albertsons.acupick.data.model.request.RecordNotificationRequestDto
import com.albertsons.acupick.data.model.request.firstInitialDotLastName
import com.albertsons.acupick.data.model.request.firstNameLastInitialDot
import com.albertsons.acupick.data.model.response.AcknowledgedPickerDetailsDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ArrivalsCountDetailsDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.ServerErrorCode.USER_NOT_VALID
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.isRxDug
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.SelectedActivities
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CloseActionListener
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.FLASH_ORDER_END_THIS_PICK_DIALOG
import com.albertsons.acupick.ui.dialog.FLASH_ORDER_END_YOUR_PICK_DIALOG
import com.albertsons.acupick.ui.dialog.FLASH_ORDER_FINISH_STAGING_THIS_DIALOG
import com.albertsons.acupick.ui.dialog.FLASH_ORDER_FINISH_STAGING_YOUR_DIALOG
import com.albertsons.acupick.ui.dialog.HANDOFF_ASSIGNED_INPROGRESS
import com.albertsons.acupick.ui.dialog.HANDOFF_FULL_DIALOG
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.MUST_COMPLETE_HANDOFF_DIALOG
import com.albertsons.acupick.ui.dialog.ORDER_NOT_AVAILABLE_GENERIC_DIALOG
import com.albertsons.acupick.ui.dialog.PARNTERPICK_ORDER_FINISH_STAGING_THIS_DIALOG
import com.albertsons.acupick.ui.dialog.PARTNERPICK_END_THIS_PICK_DIALOG
import com.albertsons.acupick.ui.dialog.PARTNERPICK_ORDER_END_YOUR_PICK_DIALOG
import com.albertsons.acupick.ui.dialog.getCustomerArrivedNotificationData
import com.albertsons.acupick.ui.dialog.getDriverArrivedDialog
import com.albertsons.acupick.ui.dialog.getDriverArrivedNotificationData
import com.albertsons.acupick.ui.dialog.getDugInterjectionAssignedHandoffNotificationArgData
import com.albertsons.acupick.ui.dialog.getDugInterjectionSkipPickingNotificationArgData
import com.albertsons.acupick.ui.dialog.getDugInterjectionSkipStagingNotificationArgData
import com.albertsons.acupick.ui.dialog.getFlashAssignedPickNotificationArgData
import com.albertsons.acupick.ui.dialog.getFlashEndHandOffNotificationArgData
import com.albertsons.acupick.ui.dialog.getFlashEndPickNotificationArgData
import com.albertsons.acupick.ui.dialog.getFlashEndStagingNotificationArgData
import com.albertsons.acupick.ui.dialog.getFlashOrderNotAvailableDialog
import com.albertsons.acupick.ui.dialog.getHandOffTakenNotificationDialog
import com.albertsons.acupick.ui.dialog.getInterjectionDailogForAllUsers
import com.albertsons.acupick.ui.dialog.getOrderNotAvailableDialog
import com.albertsons.acupick.ui.dialog.getPartnerPickOrderNotAvailableDialog
import com.albertsons.acupick.ui.models.CustomerData
import com.albertsons.acupick.ui.models.CustomerInfo
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.isDestagingFlow
import com.albertsons.acupick.ui.util.isHandOffFlow
import com.albertsons.acupick.ui.util.isPickingFlow
import com.albertsons.acupick.ui.util.isStagingFlow
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toIdHelper
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class NotificationViewModel(
    private val app: Application,
    private val activityViewModel: MainActivityViewModel,
) : BaseViewModel(app) {

    // DI
    private val apsRepo: ApsRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val pickRepo: PickRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val pushNotificationsRepo: PushNotificationsRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val userRepo: UserRepository by inject()

    val userAlreadyAssignedToMaxOrders = MutableLiveData<Boolean>()
    val addHandoffFromNotification: LiveData<Long> = LiveEvent()
    val dismissNotification: LiveData<Int> = LiveEvent()
    val dismissChatNotification: LiveData<List<String>> = LiveEvent()
    val endPickAction: LiveData<Unit> = LiveEvent()
    val notificationDialogEvent: LiveData<CustomDialogArgDataAndTag> = LiveEvent()
    val notificationNavEvent: LiveData<NavigationEvent> = LiveEvent()
    val skipToDestagingAction: LiveData<Unit> = LiveEvent()
    val addBadgeAction: LiveData<AcknowledgedPickerDetailsDto> = LiveEvent()
    val removeBadgeAction: LiveData<Unit> = LiveEvent()
    val notificationMessageSnackEvent = LiveEvent<AcupickSnackEvent>() // DUG interjection to show snackbar messages
    val badgeArrivalsAction: LiveData<ArrivalsCountDetailsDto?> = LiveEvent()

    var wasNotificationClicked = false
    private var savedNotificationData: NotificationData? = null
    private var activePickList: ActivityAndErDto? = null

    /**
     * Primary entry point for a push notification click
     */
    fun onNotificationClicked(notificationData: NotificationData, userAction: String) {
        if (notificationData.notificationType == NotificationType.CHAT || notificationData.notificationType == NotificationType.CHAT_PICKER) {
            dismissChatNotification.postValue(pushNotificationsRepo.getChatPushIds())
        } else {
            dismissNotification.postValue(notificationData.customerOrderNumber?.toIntOrNull() ?: -1)
        }
        viewModelScope.launch(dispatcherProvider.IO) {
            if (notificationData.notificationType != NotificationType.CHAT && notificationData.notificationType != NotificationType.CHAT_PICKER) {
                notifyAcceptance(notificationData, userAction)
            }
        }

        val isUserLoggedIn = userRepo.user.value != null
        if (isUserLoggedIn) {
            // handle it now
            handleNotificationAction(notificationData, false)
        } else {
            // save data to handle it after successful login
            wasNotificationClicked = true
            savedNotificationData = notificationData
        }
    }

    /**
     * Take action for a clicked notification after a successful login
     */
    fun handleNotificationAfterLogin() {
        wasNotificationClicked = false
        savedNotificationData?.let {
            handleNotificationAction(it, true)
        }
    }

    /**
     * Take action for a clicked notification - handles all types of notifications
     */
    private fun handleNotificationAction(notificationData: NotificationData, fromLogin: Boolean) {
        val currentDestinationId = pushNotificationsRepo.getCurrentDestination()
        when (notificationData.notificationType) {
            NotificationType.PICKING -> handlePickingNotificationClicked(currentDestinationId, notificationData, fromLogin)
            NotificationType.ARRIVING,
            NotificationType.ARRIVED,
            -> handleArrivalNotificationClicked(currentDestinationId, notificationData, fromLogin)
            NotificationType.CHAT,
            -> handleChatNotiication(notificationData, fromLogin, currentDestinationId)
            NotificationType.CHAT_PICKER,
            -> handleChatPickerNotiication(notificationData, fromLogin, currentDestinationId)
            else -> {}
        }
    }

    /**
     * Take appropriate action for a clicked arrival notification, based on what screen the user is on
     */
    private fun handleArrivalNotificationClicked(currentDestinationId: Int?, notificationData: NotificationData, fromLogin: Boolean) {
        when (currentDestinationId) {
            R.id.handOffFragment,
            R.id.handOffInterstitialFragment,
            -> notificationDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = MUST_COMPLETE_HANDOFF_DIALOG,
                    tag = COMPLETE_HANDOFF_DIALOG
                )
            )

            else -> startHandoff(notificationData, currentDestinationId, fromLogin)
        }
    }

    private fun navigateToPickup(activities: SelectedActivities?, customerData: CustomerData) {
        viewModelScope.launch {
            activities?.let {
                notificationNavEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToDestageOrder(it, customerData, true)
                    )
                )
            }
        }
    }

    private fun handleChatNotiication(notificationData: NotificationData, fromLogin: Boolean, currentDestinationId: Int) {
        Timber.d("handleChatNotiication")
        // TODO: may be pass order ID on notification data
        when {
            fromLogin -> navigateToHome()
            currentDestinationId.isPickingFlow() -> navigateToChatFragment(notificationData.customerOrderNumber.orEmpty())
            else -> navigateToPicklistItemFragment(notificationData.customerOrderNumber)
        }
    }

    private fun handleChatPickerNotiication(notificationData: NotificationData, fromLogin: Boolean, currentDestinationId: Int) {
        Timber.d("handleChatNotiication")
        if (currentDestinationId != R.id.chatFragment) {
            when {
                fromLogin -> navigateToHome()
                currentDestinationId.isPickingFlow() -> navigateToChatFragment(notificationData.customerOrderNumber.orEmpty())
                else -> navigateToPicklistItemFragment(notificationData.customerOrderNumber)
            }
        }
    }

    /**
     * On chat notification clicked if picker will be on home,arrival,picklist screen
     * they will navigate to picklistItemScreen first then navigate to chat screen.
     */
    private fun navigateToPicklistItemFragment(orderNumber: String?) {
        pickRepo.pickList.value?.actId?.let {
            notificationNavEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToPickListItemsFragment(activityId = it.toString(), orderNumber = orderNumber.orEmpty(), navigateToChat = true)
                )
            )
        }
    }

    /**
     * On chat notification clicked if picker will be on picklistItem,substitute,toteInfo screen
     * they will navigate to chat screen directly.
     */
    private fun navigateToChatFragment(customerOrderNumber: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val chatOrderDetail = pickRepo.pickList.value?.orderChatDetails?.firstOrNull {
                it.customerOrderNumber == customerOrderNumber
            }
            notificationNavEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToChatFragment(
                        orderNumber = customerOrderNumber,
                        convetsationId = chatOrderDetail?.conversationSid ?: conversationsRepository.getConversationId(orderId = customerOrderNumber),
                        fulfullmentOrderNumber = chatOrderDetail?.referenceEntityId.orEmpty()
                    )
                )
            )
        }
    }

    /**
     * Take appropriate action for a clicked flash picking notification, based on what screen the user is on
     */
    private fun handlePickingNotificationClicked(currentDestinationId: Int?, notificationData: NotificationData, fromLogin: Boolean) {
        when {
            // in handoff flow
            currentDestinationId.isHandOffFlow() -> {
                /** Do nothing */
            }

            // in picking flow
            currentDestinationId.isPickingFlow() -> {
                when (notificationData.serviceLevel) {
                    OrderType.FLASH -> showFlashEndThisPickDialog()
                    OrderType.FLASH3P -> showPartnerPickEndThisPickDialog()
                    else -> {}
                }
            }

            // in staging flow
            currentDestinationId.isStagingFlow() -> {
                when (notificationData.serviceLevel) {
                    OrderType.FLASH -> showFlashFinishStagingThisDialog()
                    OrderType.FLASH3P -> showPartnerPickFinishStagingThisDialog()
                    else -> {}
                }
            }

            // on home, pick lists, or arrivals screen
            else -> {
                notificationData.actId?.let { actId ->
                    tryToStartPickFromNotification(actId, fromLogin, notificationData.serviceLevel)
                }
            }
        }
    }

    private suspend fun getActivePicklist() =
        userRepo.user.value?.let { user ->
            // check if there are any pick lists assigned to me
            val result = apsRepo.searchActivities(
                userId = user.userId,
                siteId = user.selectedStoreId ?: "",
                assignedToMe = true,
                pickUpReady = false,
                hideFresh = true // this flag is always be true
            )
            if (result is ApiResult.Success) {
                return@let result.data.find { it.category == CategoryStatus.ASSIGNED_TO_ME }?.data?.firstOrNull()
            } else null
        }

    /**
     * Try to start picking an order if not currently working on (assigned to) a picking or staging activity.
     * Else, show the appropriate dialog.
     */
    private fun tryToStartPickFromNotification(actId: Long, fromLogin: Boolean, orderType: OrderType?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { tryToStartPickFromNotification(actId, fromLogin, orderType) }
            } else {
                // check if there are any pick lists assigned to me
                activePickList = getActivePicklist()

                // if not, start picking flash order
                if (activePickList == null) {
                    startPicking(actId, fromLogin, orderType)
                } else {
                    when (activePickList?.actType) {
                        // staging part 2
                        ActivityType.DROP_OFF -> {
                            when (orderType) {
                                OrderType.FLASH -> showFlashFinishStagingYourDialog()
                                OrderType.FLASH3P -> showPartnerPickFinishStagingYourDialog()
                                else -> {}
                            }
                        }

                        ActivityType.PICK_PACK -> {
                            when (activePickList?.status) {
                                // picking
                                ActivityStatus.IN_PROGRESS -> {
                                    when (orderType) {
                                        OrderType.FLASH -> showFlashEndYourPickDialog()
                                        OrderType.FLASH3P -> showPartnerPickEndYourPickDialog()
                                        else -> {}
                                    }
                                }
                                // staging part 1 is the anomaly - actType is PICK_PACK and status is COMPLETED
                                ActivityStatus.COMPLETED -> {
                                    when (orderType) {
                                        OrderType.FLASH -> showFlashFinishStagingYourDialog()
                                        OrderType.FLASH3P -> showPartnerPickFinishStagingYourDialog()
                                        else -> {}
                                    }
                                }

                                else -> {
                                    /** Do nothing */
                                }
                            }.exhaustive
                        }

                        else -> {
                            /** Do nothing */
                        }
                    }.exhaustive
                }
            }
        }
    }

    /**
     * The user has decided to end their current pick
     */
    fun endCurrentPick() {
        when (pushNotificationsRepo.getCurrentDestination()) {
            R.id.pickListItemsFragment -> endPickAction.postValue(Unit)
            else -> {
                notificationNavEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToPickListItemsFragment(
                            pickRepo.getActivePickListActivityId().toString(), navigateToChat = true
                        )
                    )
                )
                endPickAction.postValue(Unit)
            }
        }
    }

    /**
     * The user has decided to continue staging their current pick
     */
    fun navigateToStaging() {
        activePickList?.let {
            notificationNavEvent.postValue(
                NavigationEvent.Directions(
                    NavGraphDirections.actionToStagingFragment(
                        activityId = (
                            when (it.actType) {
                                // user was in staging part 1
                                ActivityType.PICK_PACK -> it.actId
                                // user was in staging part 2 - actType = DROP_OFF
                                else -> it.prevActivityId
                            }
                            ).toString(),
                        isPreviousPrintSuccessful = true,
                        shouldClearData = false
                    )
                )
            )
        }
    }

    private fun showFlashEndThisPickDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = FLASH_ORDER_END_THIS_PICK_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG
            )
        )
    }

    private fun showFlashEndYourPickDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = FLASH_ORDER_END_YOUR_PICK_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG
            )
        )
    }

    private fun showFlashFinishStagingThisDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = FLASH_ORDER_FINISH_STAGING_THIS_DIALOG,
                tag = ""
            )
        )
    }

    private fun showFlashFinishStagingYourDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = FLASH_ORDER_FINISH_STAGING_YOUR_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_FINISH_STAGING_DIALOG_TAG
            )
        )
    }

    private fun showPartnerPickEndThisPickDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = PARTNERPICK_END_THIS_PICK_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG
            )
        )
    }

    private fun showPartnerPickEndYourPickDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = PARTNERPICK_ORDER_END_YOUR_PICK_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG
            )
        )
    }

    private fun showPartnerPickFinishStagingThisDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = PARNTERPICK_ORDER_FINISH_STAGING_THIS_DIALOG,
                tag = ""
            )
        )
    }

    private fun showPartnerPickFinishStagingYourDialog() {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = FLASH_ORDER_FINISH_STAGING_YOUR_DIALOG,
                tag = FLASH_AND_PARTNERPICK_ORDER_FINISH_STAGING_DIALOG_TAG
            )
        )
    }

    fun displayHybridPickingDialog(picker: String?, data: NotificationData) {
        viewModelScope.launch(dispatcherProvider.IO) {
            savedNotificationData = data
            activePickList = getActivePicklist()
            val currentDestinationId = pushNotificationsRepo.getCurrentDestination()
            val currentActType = activePickList?.actType

            notificationDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = when {
                        currentDestinationId.isHandOffFlow() -> {
                            hybridModalPositiveAction = {
                                acceptNotification(data, FLASH_ORDER_ACKNOWLEDGED_USER_ACTION)
                            }
                            getFlashEndHandOffNotificationArgData(picker.orEmpty())
                        }

                        currentActType == ActivityType.PICK_PACK -> {
                            when (activePickList?.status) {
                                ActivityStatus.COMPLETED -> {
                                    hybridModalPositiveAction = {
                                        // navigateToStaging()
                                        acceptNotification(data, FLASH_ORDER_ACKNOWLEDGED_USER_ACTION)
                                    }
                                    getFlashEndStagingNotificationArgData(picker.orEmpty())
                                }

                                else -> {
                                    hybridModalPositiveAction = {
                                        // endCurrentPick()
                                        acceptNotification(data, FLASH_ORDER_ACKNOWLEDGED_USER_ACTION)
                                    }
                                    getFlashEndPickNotificationArgData(picker.orEmpty())
                                }
                            }.exhaustive
                        }

                        currentActType == ActivityType.DROP_OFF -> {
                            hybridModalPositiveAction = {
                                // navigateToStaging()
                                acceptNotification(data, FLASH_ORDER_ACKNOWLEDGED_USER_ACTION)
                            }
                            getFlashEndStagingNotificationArgData(picker.orEmpty())
                        }

                        else -> {
                            hybridModalPositiveAction = {
                                // data.actId?.let { startPicking(it, false, data.serviceLevel) }
                                acceptNotification(data, FLASH_ORDER_ACCEPTED_USER_ACTION)
                            }
                            getFlashAssignedPickNotificationArgData(picker.orEmpty())
                        }
                    },
                    tag = FLASH_INTERJECTION_DIALOG_TAG
                )
            )
            hybridModalNegativeAction = {
                savedNotificationData?.let { data -> acceptNotification(data, FLASH_ORDER_REJECTED_USER_ACTION) }
                savedNotificationData = null
            }
            hybridModalCloseAction = {
                savedNotificationData?.let { data -> acceptNotification(data, FLASH_ORDER_REJECTED_USER_ACTION) }
                savedNotificationData = null
            }
        }
    }

    fun handleUrgentOrFlashArrival(data: NotificationData) {
        if (data.isFinalNotification()) {
            displayUrgentDriverArrivedNotification(data)
        } else {
            if (data.serviceLevel == OrderType.FLASH || data.serviceLevel == OrderType.FLASH3P) {
                showFlashDriverArrivedDialog(data)
            }
        }
    }

    private fun shouldDisplayUrgentDriverDialog(currentDestinationId: Int?) = !currentDestinationId.isHandOffFlow() ||
        (currentDestinationId == R.id.destageOrderFragment && !(userAlreadyAssignedToMaxOrders.value ?: false))

    private fun displayUrgentDriverArrivedNotification(data: NotificationData) {
        viewModelScope.launch {
            val currentDestinationId = pushNotificationsRepo.getCurrentDestination()
            val customer = data.asFirstInitialDotLastString()
            val activityDetailsResult = isBlockingUi.wrap {
                pickRepo.getActivityDetails(
                    data.actId.toString().also {
                        it.logError(
                            "Null Activity Id. NotificationViewModel(displayUrgentDriverArrivedNotification)," +
                                " Order Id-${data.customerOrderNumber}, User Id-${userRepo.user.value?.userId}, storeId-${data.siteId}"
                        )
                    }
                )
            }
            val partner = (activityDetailsResult as? ApiResult.Success)?.data?.partnerName ?: "The"
            val driver = (activityDetailsResult as? ApiResult.Success)?.data?.driver?.asFirstInitialDotLastString() ?: "The driver"

            if (shouldDisplayUrgentDriverDialog(currentDestinationId)) {
                notificationDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = if (data.fulfillmentType == FulfillmentType.DUG) {
                            getCustomerArrivedNotificationData(customer)
                        } else {
                            getDriverArrivedNotificationData(partner, driver, customer)
                        },
                        tag = URGENT_DRIVER_ARRIVED_DIALOG
                    )
                )
                hybridModalPositiveAction = {
                    startHandoff(data, currentDestinationId, currentDestinationId == R.id.loginFragment)
                }
            }
        }
    }

    fun handleDugInterjection(data: NotificationData) {
        // TODO: Optimize the code by creating a method for all repetation if else condtions
        // This method handels both Arriaval Interjection and  Arriaval Interjection for all users

        val isInterjectionForAllUsers = data.notificationType == NotificationType.ARRIVED_INTERJECTION_ALL_USER
        viewModelScope.launch {
            val currentDestinationId = pushNotificationsRepo.getCurrentDestination()
            savedNotificationData = data // TODO: Saving it as per FLASH interjection if its nor required will remove it
            activePickList = getActivePicklist()
            val currentActType = activePickList?.actType

            fun startHandOffAndResetInterjectionVariables() {
                startHandoff(notificationData = data, currentDestinationId = currentDestinationId, fromLogin = false)
                resetDugInterjectionVariables()
            }

            fun closeActionDugInterjection() {
                savedNotificationData?.let { data ->
                    handleCloseActionDugInterjectionDialog(data, DUG_HANDOFF_REJECTED_USER_ACTION)
                }
            }
            notificationDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = when {
                        currentActType == ActivityType.PICK_PACK -> {
                            when (activePickList?.status) {
                                ActivityStatus.COMPLETED -> {
                                    hybridModalNegativeAction = { startHandOffAndResetInterjectionVariables() }
                                    hybridModalPositiveAction = {
                                        savedNotificationData?.let { data ->
                                            handleCloseActionDugInterjectionDialog(data, DUG_HANDOFF_REJECTED_USER_ACTION)
                                        }
                                    }
                                    if (isInterjectionForAllUsers) {
                                        getInterjectionDailogForAllUsers(data, R.string.flash_order_continue_staging_button.toIdHelper())
                                    } else {
                                        getDugInterjectionSkipStagingNotificationArgData(data, activePickList?.assignedTo?.firstName)
                                    }
                                }

                                else -> {
                                    hybridModalPositiveAction = { startHandOffAndResetInterjectionVariables() }
                                    if (isInterjectionForAllUsers) {
                                        getInterjectionDailogForAllUsers(data)
                                    } else {
                                        getDugInterjectionSkipPickingNotificationArgData(data, activePickList?.assignedTo?.firstName)
                                    }
                                }
                            }.exhaustive
                        }

                        currentActType == ActivityType.DROP_OFF -> {
                            hybridModalNegativeAction = {
                                if (isInterjectionForAllUsers) {
                                    closeActionDugInterjection()
                                } else {
                                    startHandOffAndResetInterjectionVariables()
                                }
                            }
                            hybridModalPositiveAction = {
                                if (isInterjectionForAllUsers) {
                                    startHandOffAndResetInterjectionVariables()
                                } else {
                                    closeActionDugInterjection()
                                }
                            }
                            if (isInterjectionForAllUsers) {
                                getInterjectionDailogForAllUsers(data, R.string.flash_order_continue_staging_button.toIdHelper())
                            } else {
                                getDugInterjectionSkipStagingNotificationArgData(data, activePickList?.assignedTo?.firstName)
                            }
                        }

                        else -> {
                            hybridModalPositiveAction = { startHandOffAndResetInterjectionVariables() }
                            if (isInterjectionForAllUsers) {
                                getInterjectionDailogForAllUsers(data)
                            } else {
                                getDugInterjectionAssignedHandoffNotificationArgData(data, userRepo.user.value?.firstName)
                            }
                        }
                    },
                    tag = if (isInterjectionForAllUsers) {
                        INTERJECTION_FOR_ALL_USER_DIALOG_TAG
                    } else {
                        DUG_INTERJECTION_DIALOG_TAG
                    }
                )
            ).also { pushNotificationsRepo.setDugInterjectionState(DugInterjectionState.Appear(data.customerOrderNumber)) }
        }
    }

    private fun handleCloseActionDugInterjectionDialog(notificationData: NotificationData, userAction: String) {
        acceptNotification(notificationData, userAction)
        resetDugInterjectionVariables()
        savedNotificationData = null
    }

    // To reset initial state of dugInterjectionStateReason on positive and negative actions
    private fun resetDugInterjectionVariables() {
        pushNotificationsRepo.setDugInterjectionState(DugInterjectionState.None)
    }

    private var hybridModalPositiveAction: (() -> Unit)? = null
    private var hybridModalNegativeAction: (() -> Unit)? = null
    private var hybridModalCloseAction: (() -> Unit)? = null
    val notifcationListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            // if (siteRepo.isHybridPickingDialogEnabled) {
            when (closeAction) {
                CloseAction.Positive -> hybridModalPositiveAction?.invoke()
                CloseAction.Negative -> hybridModalNegativeAction?.invoke()
                else -> {}
            }
            /* } else {
                 when (closeAction) {
                     CloseAction.Negative -> {
                         savedNotificationData?.let { data -> acceptNotification(data, FLASH_ORDER_REJECTED_USER_ACTION) }
                         savedNotificationData = null
                     }
                     else -> {}
                 }
             }*/
        }
    }

    private fun acceptNotification(data: NotificationData, userAction: String) {
        viewModelScope.launch(dispatcherProvider.IO) {
            notifyAcceptance(data, userAction)
        }
    }

    private fun showFlashDriverArrivedDialog(data: NotificationData) {
        notificationDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = getDriverArrivedDialog(data),
                tag = FLASH_DRIVER_ARRIVED_TAG
            )
        )
    }

    /**
     * Try to start picking a pick list from a clicked notification
     */
    private fun startPicking(actId: Long, fromLogin: Boolean, orderType: OrderType?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.value.not()) {
                networkAvailabilityManager.triggerOfflineError { startPicking(actId, fromLogin, orderType) }
            } else {
                // if pick list is already assigned, get its assignee's name
                val activityDetailsResult = isBlockingUi.wrap {
                    pickRepo.getActivityDetails(actId.toString())
                }
                val pickerName = (activityDetailsResult as? ApiResult.Success)?.data?.assignedTo?.firstInitialDotLastName()

                // try to assign pick list to myself
                when (
                    val result = isBlockingUi.wrap {
                        pickRepo.assignUser(
                            AssignUserRequestDto(
                                actId = actId,
                                replaceOverride = false,
                                user = userRepo.user.value?.toUserDto(),
                                defaultPickListSelected = true,
                                tokenizedLdapId = userRepo.user.value?.tokenizedLdapId
                            )
                        )
                    }
                ) {
                    is ApiResult.Success -> {
                        notificationNavEvent.postValue(
                            NavigationEvent.Directions(
                                NavGraphDirections.actionToPickListItemsFragment(actId.toString())
                            )
                        )
                    }

                    is ApiResult.Failure -> {
                        if (result is ApiResult.Failure.Server) {
                            if (fromLogin) navigateToHome()
                            when (result.error?.errorCode?.resolvedType) {
                                ServerErrorCode.NO_OVER_RIDE_FLAG -> {
                                    val dialogData = pickerName?.let {
                                        when (activityDetailsResult.data.orderType) {
                                            OrderType.FLASH -> getFlashOrderNotAvailableDialog(it)
                                            OrderType.FLASH3P -> getPartnerPickOrderNotAvailableDialog(it)
                                            else -> getOrderNotAvailableDialog(it)
                                        }
                                    } ?: run { ORDER_NOT_AVAILABLE_GENERIC_DIALOG }

                                    notificationDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = dialogData,
                                            tag = ""
                                        )
                                    )
                                }

                                ServerErrorCode.USER_ALREADY_ASSIGNED_TO_AN_ACTIVITY -> {
                                    when (orderType) {
                                        OrderType.FLASH -> showFlashEndYourPickDialog()
                                        OrderType.FLASH3P -> showPartnerPickEndYourPickDialog()
                                        else -> {}
                                    }
                                }

                                else -> ORDER_NOT_AVAILABLE_GENERIC_DIALOG
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Try to start handing off a order from a clicked notification
     */
    // TODO - Clean up this method
    private fun startHandoff(notificationData: NotificationData, currentDestinationId: Int?, fromLogin: Boolean) {
        viewModelScope.launch(dispatcherProvider.Main) {
            val user = userRepo.user.value ?: run {
                return@launch // if not logged in
            }
            // ACIP-203884 -> Logging additional info in AppD to analyze how storeId going as null for this Api call.
            user.selectedStoreId.toString().logError(
                "Null StoreId. NotificationViewModel(startHandoff), " +
                    "OrderId-${notificationData.customerOrderNumber}, UserId-${user.userId}, siteId-${user.selectedStoreId}, isHyb-${notificationData.isShowingHybridMessage}"
            )

            activityViewModel.setLoadingState(isLoading = true, blockUi = true)
            val result = apsRepo.searchCustomerPickupOrders(
                firstName = null,
                lastName = null,
                orderNumber = notificationData.customerOrderNumber,
                siteId = user.selectedStoreId,
                onlyPickupReady = true,
            )

            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    when (result) {
                        is ApiResult.Success -> {
                            result.data.firstOrNull()?.let { orderInNotification ->
                                if (orderInNotification.assignedTo == null || orderInNotification.assignedTo?.userId == user.userId) {
                                    if (currentDestinationId.isDestagingFlow() && userAlreadyAssignedToMaxOrders.value == true) {
                                        notificationDialogEvent.postValue(
                                            CustomDialogArgDataAndTag(
                                                data = HANDOFF_FULL_DIALOG,
                                                tag = DestageOrderPagerViewModel.MAX_ORDERS_ASSIGNED
                                            )
                                        )
                                    } else {
                                        val customerInfo = CustomerInfo(
                                            "${orderInNotification.contactPersonDto?.firstName} ${
                                            orderInNotification
                                                .contactPersonDto?.lastName
                                            }".trim(),
                                            orderInNotification.contactPersonDto?.id.orEmpty(), orderInNotification.erId ?: 0L
                                        )
                                        loadSelectionDetails(notificationData, currentDestinationId, orderInNotification.erId ?: 0L, CustomerData(listOf(customerInfo)))
                                    }
                                } else {
                                    if (fromLogin) navigateToHome()
                                    val userAssigned = orderInNotification.assignedTo?.firstNameLastInitialDot()
                                    acceptNotification(notificationData, ACCEPT_FAILURE)
                                    notificationDialogEvent.postValue(
                                        CustomDialogArgDataAndTag(
                                            data = getHandOffTakenNotificationDialog(userAssigned),
                                            tag = HANDOFF_ALREADY_ASSIGNED
                                        )
                                    )
                                }
                            }
                        }

                        is ApiResult.Failure -> {
                            if (fromLogin) navigateToHome()
                            handleApiError(result)
                        }
                    }.exhaustive
                }

                false -> networkAvailabilityManager.triggerOfflineError { startHandoff(notificationData, currentDestinationId, fromLogin) }
            }
            activityViewModel.setLoadingState(isLoading = false, blockUi = false)
        }
    }

    private suspend fun loadSelectionDetails(notificationData: NotificationData, currentDestinationId: Int?, erId: Long, customerData: CustomerData) {
        val result = apsRepo.pickUpActivityDetails(id = erId, loadCI = true)
        when (result) {
            is ApiResult.Success<ActivityDto> -> {
                /**
                 * Validting if picker destaging non Rx order and DUG interjection comes. First will check the incoming DUG interjection is for an Rx order or not.
                 * If it is an Rx DUG interjection app will send [DugInterjectionState.FE_BATCH_FAILURE] exception to the server.
                 * If it is a normal DUG interjection then the handoff will be assign to that picker.
                 */
                if (currentDestinationId.isDestagingFlow().orFalse() && result.data.isRxDug().orFalse()) {
                    delegateMessageEvent(AcupickSnackEvent(message = StringIdHelper.Id(R.string.batch_failure_message_for_rx_order), type = SnackType.ERROR))
                    pushNotificationsRepo.setDugInterjectionState(DugInterjectionState.BatchFailureReason.DestagingRxOrder)
                    recordNotificationReceived(notificationData)
                    return
                }
                /**
                 * Validating if picker is on destaging sub flow then we are navigating up and
                 * post the actId to destageOrderFragment to load the new incoming order detail.
                 */
                if (currentDestinationId.isDestagingFlow() && userAlreadyAssignedToMaxOrders.value == false) {
                    if (currentDestinationId != R.id.destageOrderFragment) {
                        notificationNavEvent.postValue(NavigationEvent.Up)
                        delay(200)
                    }
                    addHandoffFromNotification.postValue(notificationData.actId)
                    sendAcceptanceDugInterjection(notificationData, DUG_HANDOFF_ACCEPTED_USER_ACTION)
                    return
                }
                assignToMe(notificationData, result.data, result.data.actId ?: 0L, false, customerData)
            }

            is ApiResult.Failure -> {
                if (result is ApiResult.Failure.Server) {
                    val type = result.error?.errorCode?.resolvedType
                    if (type == ServerErrorCode.USER_NOT_VALID) {
                        notificationDialogEvent.postValue(
                            CustomDialogArgDataAndTag(
                                data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                                tag = DestageOrderPagerViewModel.HANDOFF_ALREADY_ASSIGNED_DIALOG_TAG,
                            )
                        )
                    } else {
                        handleApiError(result) // , retryAction = { handleNotificationAction(notificationData, fromLogin) })
                    }
                } else {
                    handleApiError(result) // , retryAction = { handleNotificationAction(notificationData, fromLogin) })
                }
            }
        }.exhaustive
    }

    /**
     * This method will post the message, other required fragments observing this event
     * and will show snackbar message on the screen.
     */
    private fun delegateMessageEvent(acupickSnackEvent: AcupickSnackEvent) {
        notificationMessageSnackEvent.postValue(acupickSnackEvent)
    }

    private suspend fun assignToMe(notificationData: NotificationData, activityDto: ActivityDto, actId: Long, shouldOverride: Boolean, customerData: CustomerData) {
        val result = apsRepo.assignUserToHandoffs(
            AssignUserWrapperRequestDto(
                actIds = listOf(actId),
                replaceOverride = shouldOverride,
                // Todo find out what this does
                resetPickList = true,
                user = userRepo.user.value?.toUserDto(),
                etaArrivalFlag = siteRepo.isEtaArrivalEnabled
            )
        )
        when (result) {
            is ApiResult.Success<List<ActivityDto>> -> {
                sendAcceptanceDugInterjection(notificationData, DUG_HANDOFF_ACCEPTED_USER_ACTION)
                navigateToPickup(SelectedActivities(activityList = listOf(activityDto)), customerData)
            }

            is ApiResult.Failure -> {
                if (result is ApiResult.Failure.Server) {
                    val type = result.error?.errorCode?.resolvedType
                    val isBatch = false // selectedItemIds.value.size > 1
                    when (type) {
                        ServerErrorCode.OPTIMISTIC_LOCKING_ERROR, ServerErrorCode.NO_OVER_RIDE_FLAG, USER_NOT_VALID -> {
                            acceptNotification(notificationData, ACCEPT_FAILURE)
                            notificationDialogEvent.postValue(
                                CustomDialogArgDataAndTag(
                                    data = HANDOFF_ASSIGNED_INPROGRESS,
                                    tag = HANDOFF_ASSIGNED_INPROGRESS_TAG
                                )
                            )
                        }
                        ServerErrorCode.NO_USER_TO_ASSIGN_ACTIVITY, ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY -> {
                            serverErrorCannotAssignUser(CannotAssignToOrderDialogTypes.HANDOFF, isBatch)
                        }
                        else -> {
                            handleApiError(result) // , retryAction = { assignToMe(activityDto, actId, shouldOverride, customerData) })
                        }
                    }
                } else {
                    handleApiError(result) // , retryAction = { assignToMe(activityDto, actId, shouldOverride, customerData) })
                }
            }
        }
    }

    /**
     * Acceptance would be sent once handoff assigned successfully in case of DUG interjection only.
     * For the general ARRIVED/ARRIVING notification acceptance will be sent at the time of notification clicked.
     */
    private fun sendAcceptanceDugInterjection(notificationData: NotificationData, userAction: String) {
        if (notificationData.notificationType == NotificationType.ARRIVED_INTERJECTION || notificationData.notificationType == NotificationType.ARRIVED_INTERJECTION_ALL_USER) {
            acceptNotification(notificationData, userAction)
        }
    }

    fun navigateToArrivals() {
        notificationNavEvent.postValue(
            NavigationEvent.Directions(
                NavGraphDirections.actionToArrivalsPagerFragment()
            )
        )
    }

    fun skipToDestaging() {
        when (pushNotificationsRepo.getCurrentDestination()) {
            R.id.stagingPart2Fragment -> skipToDestagingAction.postValue(Unit)
            else -> {
                viewModelScope.launch(dispatcherProvider.IO) {
                    // If user is on a secondary screen in staging part 2 (manual entry, add bags, reprint labels),
                    // return to staging part 2
                    notificationNavEvent.postValue(NavigationEvent.Up)
                    // TODO: This delay allows navigation up to staging part 2 enough time to happen before skipping destaging. Find a better way to do this
                    delay(500)
                    skipToDestagingAction.postValue(Unit)
                }
            }
        }
    }

    fun checkForAcknowledgedFlashOrder() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first()) {
                userRepo.user.value?.userId?.let { userId ->
                    val result = apsRepo.getAcknowledgedPickerDetails(userId)
                    if ((result as? ApiResult.Success)?.data?.isFlashOrderAcknowledged == true) {
                        addBadgeAction.postValue(result.data)
                        return@launch
                    }
                    removeBadgeAction.postValue(Unit)
                }
            }
        }
    }

    fun checkForArrivedOrders() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first()) {
                userRepo.user.value?.selectedStoreId?.let { siteId ->
                    val result = apsRepo.getCustomerArrivalsCount(siteId)
                    (result as? ApiResult.Success)?.data?.let {
                        if (it.customerArrivalsCount.getOrZero() > 0) badgeArrivalsAction.postValue(it)
                        else badgeArrivalsAction.postValue(null)
                        return@launch
                    }
                    badgeArrivalsAction.postValue(null)
                }
            }
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun notifyAcceptance(notificationData: NotificationData, userAction: String) {
        if (networkAvailabilityManager.isConnected.first() && userRepo.isLoggedIn.value) {
            apsRepo.acceptNotification(
                NotificationRequestDto(
                    actId = notificationData.actId, // make this API call even if actId is null for some reason
                    deviceId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID),
                    notificationCounter = notificationData.notificationCounter,
                    notificationType = notificationData.notificationType,
                    acceptedTimeStamp = ZonedDateTime.now(),
                    userAction = userAction,
                    isAutoAssign = userAction == FLASH_ORDER_ACCEPTED_USER_ACTION,
                    userId = userRepo.user.value?.userId
                )
            )
            // We don't care about the result of the API call
        }
    }

    /**
     * DUG interjection call this method while sending exception
     * in case of Batch failure at the time of add another handoff button click from interjection dialog.
     */
    private fun recordNotificationReceived(data: NotificationData) {
        viewModelScope.launch(dispatcherProvider.IO) {
            notifyAcknowledgement(data)
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun notifyAcknowledgement(data: NotificationData) {
        if (networkAvailabilityManager.isConnected.first() && userRepo.isLoggedIn.value) {
            pushNotificationsRepo.recordNotificationReceived(
                RecordNotificationRequestDto(
                    actId = data.actId,
                    deviceId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID),
                    notificationCounter = data.getNotificationReceivedCount(),
                    notificationType = data.notificationType,
                    receivedTimeStamp = ZonedDateTime.now(),
                    failureReasonCode = when (data.notificationType) {
                        NotificationType.ARRIVED_INTERJECTION ->
                            pushNotificationsRepo.getDugInterjectionState().failureReasonTextValue()

                        else -> null
                    }
                )
            )
            resetDugInterjectionVariables()
        }
    }

    fun persistAndSendFcmToken(token: String) {
        pushNotificationsRepo.saveFcmToken(token)
        if (userRepo.isLoggedIn.value && userRepo.user.value?.selectedStoreId != null) {
            viewModelScope.launch(dispatcherProvider.IO) {
                withContext(NonCancellable) {
                    pushNotificationsRepo.sendFcmTokenToBackend()
                }
            }
        }
    }

    companion object {
        const val HANDOFF_ALREADY_ASSIGNED = "handOffReadyToAssign"
        const val COMPLETE_HANDOFF_DIALOG = "completeHandoffDialog"
        const val FLASH_AND_PARTNERPICK_ORDER_FINISH_STAGING_DIALOG_TAG = "flashOrderFinishStagingDialogTag"
        const val FLASH_AND_PARTNERPICK_ORDER_END_PICK_DIALOG_TAG = "flashAndPartnerPickOrderEndPickDialogTag"
        const val FLASH_DRIVER_ARRIVED_TAG = "flashDriverArrivedTag"
        const val FLASH_INTERJECTION_DIALOG_TAG = "flashInterjectionDialogTag"
        const val FLASH_ORDER_ACKNOWLEDGED_USER_ACTION = "FLASH_ORDER_ACKNOWLEDGED"
        const val FLASH_ORDER_REJECTED_USER_ACTION = "FLASH_ORDER_REJECTED"
        const val ACCEPT_FAILURE = "ACCEPT_FAILURE"
        const val DUG_HANDOFF_REJECTED_USER_ACTION = "REJECTED"
        const val FLASH_ORDER_ACCEPTED_USER_ACTION = "ACCEPTED"
        const val DUG_HANDOFF_ACCEPTED_USER_ACTION = "ACCEPTED"
        const val URGENT_DRIVER_ARRIVED_DIALOG = "urgentDriverArrivedDialog"
        const val DUG_INTERJECTION_DIALOG_TAG = "dugArrivalInterjectionDialogTag"
        const val INTERJECTION_FOR_ALL_USER_DIALOG_TAG = "interjectionForAllUsers"
        const val HANDOFF_ASSIGNED_INPROGRESS_TAG = "handOffAssignedInprogress"
    }
}
