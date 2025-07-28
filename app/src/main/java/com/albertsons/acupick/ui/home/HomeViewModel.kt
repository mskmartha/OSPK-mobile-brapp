package com.albertsons.acupick.ui.home

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.albertsons.acupick.AcuPickConfig
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.getCustomerType
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.CustomerType
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.OrderByCountType
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.Get1PLTruckRemovalItemListRequestDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.CannotAssignToOrderDialogTypes
import com.albertsons.acupick.data.model.response.FE_SCREEN_STATUS_STORE_NOTIFIED
import com.albertsons.acupick.data.model.response.PickListBatchingType
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.albertsons.acupick.data.model.response.WineStagingType
import com.albertsons.acupick.data.model.response.fulfillmentTypes
import com.albertsons.acupick.data.model.shouldShowCountdownTimer
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.GamePointsRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.domain.extensions.throttleFirst
import com.albertsons.acupick.image.ImagePreCacher
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.isValidActivityId
import com.albertsons.acupick.infrastructure.utils.toSameZoneInstantLocalDate
import com.albertsons.acupick.infrastructure.utils.toZoneTime
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.ArrivalsViewModel
import com.albertsons.acupick.ui.arrivals.SelectedActivities
import com.albertsons.acupick.ui.arrivals.destage.DestageOrderPagerViewModel
import com.albertsons.acupick.ui.dialog.BaseCustomDialogFragment
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.HAND_OFF_ALREADY_ASSIGNED_ARG_DATA
import com.albertsons.acupick.ui.dialog.OF_AGE_ASSOCIATE_VERIFICATION_DATA
import com.albertsons.acupick.ui.dialog.RELOAD_DILAOG
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.FulfillmentTypeUI
import com.albertsons.acupick.ui.staging.winestaging.BoxUiData
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.combineWith
import com.albertsons.acupick.ui.util.firstInitialDotLastName
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.getSizedImageUrl
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.storeNumberTitle
import com.albertsons.acupick.ui.util.toFulfillmentTypeUI
import com.albertsons.acupick.usecase.handoff.CompleteHandoff1PLUseCase
import com.albertsons.acupick.usecase.handoff.CompleteHandoffUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import timber.log.Timber
import java.sql.Time
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class HomeViewModel(
    private val app: Application,
    private val userRepo: UserRepository,
    val apsRepo: ApsRepository,
    private val pickRepository: PickRepository,
    val dispatcherProvider: DispatcherProvider,
    private val networkAvailabilityManager: NetworkAvailabilityManager,
    private val completeHandoffUseCase: CompleteHandoffUseCase,
    private val completeHandoff1PLUseCase: CompleteHandoff1PLUseCase,
    private val activityViewModel: MainActivityViewModel,

) : BaseViewModel(app) {

    private val siteRepository: SiteRepository by inject()
    private val wineStagingStateRepo: WineShippingStageStateRepository by inject()
    private val imagePreCacher: ImagePreCacher by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    private val idRepository: IdRepository by inject()
    private val apiCallTimeStamp: GamePointsRepository by inject()
    // Loading flags
    val isDataLoading: LiveData<Boolean> = MutableLiveData(true)
    val isDataRefreshing = MutableLiveData(false)
    val showSkeletonState = combine(isDataLoading.asFlow(), isDataRefreshing.asFlow()) { loading, refreshing ->
        loading && refreshing.not()
    }.asLiveData()
    val showTimerPastDue = MutableLiveData(false)

    private var timerJob: Job? = null

    // Pick list item drives maps to many other UI fields.
    val cardData: LiveData<HomeCardData?> = MutableLiveData()
    val pickListType: LiveData<PickListBatchingType> = MutableLiveData(PickListBatchingType.SingleOrder)
    val orderCount: LiveData<Int> = MutableLiveData(1)
    val activityFulfillmentTypes: LiveData<Set<FulfillmentTypeUI>> = MutableLiveData(emptySet())
    val vanNumber: LiveData<String> = MutableLiveData("")
    val orderType: LiveData<OrderType> = MutableLiveData()
    private val isSnap = MutableStateFlow(false)
    private val isSubscription = MutableStateFlow(false)
    val isCattEnabled = AcuPickConfig.cattEnabled
    val customerTypeIcon: LiveData<CustomerType?> = combine(isSnap, isSubscription) { isEbt, isFreshPass ->
        getCustomerType(isEbt, isFreshPass)
    }.asLiveData()
    val timerStillActive = MutableLiveData(true)
    val isBatchOrder = pickListType.map { it == PickListBatchingType.Batch }
    val hideTimer = MutableLiveData<Boolean>(false)
    val dueDay = MutableLiveData(0L)
    val source = cardData.map { cd ->
        if (cd?.vanNumber.isNotNullOrEmpty()) app.getString(R.string.one_pl_van_number, cd?.vanNumber.orEmpty())
        else if (cd?.isOrderReadyToPickUp == true && cd.is3p == true) cd.source
        else ""
    }

    // Map item to storage type indicators
    val ambientActive: LiveData<Boolean> = cardData.map { item -> item?.storageTypes?.any { it == StorageType.AM } ?: false }
    val coldActive: LiveData<Boolean> = cardData.map { item -> item?.storageTypes?.any { it == StorageType.CH } ?: false }
    val frozenActive: LiveData<Boolean> = cardData.map { item -> item?.storageTypes?.any { it == StorageType.FZ } ?: false }
    val hotActive: LiveData<Boolean> = cardData.map { item -> item?.storageTypes?.any { it == StorageType.HT } ?: false }

    val associateName: LiveData<String> = userRepo.user.map { app.getString(R.string.hello_associate_name, it?.firstName.orEmpty()) }.asLiveData()
    val picklistIllustrationDrawable: LiveData<Int?> = combine(
        orderType.asFlow(), cardData.asFlow(), activityFulfillmentTypes.asFlow(),
        isCattEnabled, customerTypeIcon.asFlow(), ::getIllustrationDrawable
    ).asLiveData()

    // Map user to display name
    val displayName: LiveData<String> = userRepo.user.map { it?.firstInitialDotLastName() ?: "" }.asLiveData()
    val storeTitle: LiveData<String> = userRepo.user.map { it?.storeNumberTitle() ?: "" }.asLiveData()

    private val autoInitiated = siteRepository.isAutoInitiated

    val loadingState: LiveData<HomeLoadingState> = MutableLiveData(if (autoInitiated) HomeLoadingState.Initial else HomeLoadingState.End)

    val associateCta: LiveData<String> = cardData.combineWith(loadingState) { cardData, state ->
        when {
            cardData == null && state == HomeLoadingState.Initial -> app.getString(R.string.empty)
            cardData == null && state == HomeLoadingState.Intermediate -> app.getString(R.string.we_are_still_loading_the_order)
            cardData == null && state == HomeLoadingState.End -> app.getString(R.string.there_is_no_task_right_now)
            cardData == null -> app.getString(R.string.there_is_no_task_right_now)
            cardData.is1Pl == true -> app.getString(R.string.here_is_your_next_task)
            cardData.isOrderInStagePhase -> app.getString(R.string.continue_staging_order)
            cardData.isOrderReadyToPickUp -> app.getString(R.string.here_is_your_next_task)
            cardData.isAssignedToMe -> app.getString(R.string.hello_continue_pick)
            else -> app.getString(R.string.here_is_your_next_pick)
        }
    }

    val pickingText: LiveData<String> = cardData.map { cardData ->
        when {
            cardData?.is1Pl == true -> app.getString(R.string.handoff_cta)
            cardData?.isAssignedToMe == true && cardData.isOrderInStagePhase -> app.getString(R.string.start_staging)
            cardData?.isAssignedToMe == true && !cardData.isOrderInStagePhase -> app.getString(R.string.continue_picking_home_cta)
            cardData?.isAssignedToMe == false -> app.getString(R.string.start_picking)
            else -> app.getString(R.string.empty)
        }
    }

    var nextPickTitle: LiveData<String> = MutableLiveData()

    // Misc UI
    val openOrderCount: LiveData<String> = MutableLiveData()
    val handOffOrderCount: LiveData<String> = MutableLiveData()
    val dugCount: LiveData<String> = MutableLiveData()
    val deliveryCount: LiveData<String> = MutableLiveData()
    val waitTimeSeconds: LiveData<Long> = MutableLiveData()

    /*An intial value is given to the timer so that the the cta and timer don't
         turn red before the data is successfully loaded*/
    val countdownDurationMs = MutableLiveData(730000L)
    val isOrderMfc: LiveData<Boolean> = MutableLiveData(false)
    var concernTimeMs: Long = siteRepository.concernTime
    var warningTimeMs: Long = siteRepository.warningTime
    var timeSinceOrderReleasedMs: Long = 0L
    val timerColor: LiveData<Int> = MutableLiveData(R.color.grey_700)
    private val releaseTime: LiveData<ZonedDateTime> = MutableLiveData()
    private val handOffResult = MutableStateFlow(ActivityDto())

    // internal state
    private var selectedPickListActivityId: String? = null
    val hideDivider = isBatchOrder.combineWith(cardData) { isBatch, data ->
        isBatch == true && data?.is1Pl != true
    }
    val hidePastDue = cardData.map {
        if (it?.is1Pl == true) ChronoUnit.SECONDS.between(ZonedDateTime.now(), it.vanDepartureTime).let { due -> due >= 0 }
        else true
    }

    private var intervalTimer: Job? = null
    val emptyStateDrawable: LiveData<Drawable?> = cardData.combineWith(loadingState) { cardData, state ->
        if (cardData == null) {
            state?.drawableId?.get(app.applicationContext)
        } else null
    }

    val emptyStateText: LiveData<String?> = cardData.combineWith(loadingState) { cardData, state ->
        if (cardData == null) {
            state?.textResourceId?.let(app::getString)
        } else null
    }

    init {

        // Use View model scope to observe so we don't have to remover observers from live data
        refreshTotalGamePoints()
        showFirstLaunchDialog()
        viewModelScope.launch {
            completeHandoffUseCase.handOffReassigned.collect {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                        tag = SINGLE_ORDER_ERROR_DIALOG_TAG
                    )
                )
            }
        }

        viewModelScope.launch {
            completeHandoff1PLUseCase.handOffReassigned.collect {
                inlineDialogEvent.postValue(
                    CustomDialogArgDataAndTag(
                        data = HAND_OFF_ALREADY_ASSIGNED_ARG_DATA,
                        tag = SINGLE_ORDER_ERROR_DIALOG_TAG
                    )
                )
            }
        }

        registerCloseAction(ArrivalsViewModel.OF_AGE_ASSOCIATE_VERIFICATION_TAG) {
            closeActionFactory(
                positive = {
                    navigateToPickup(handOffResult.value)
                    handOffResult.value = ActivityDto()
                },
                negative = {
                    unassignPickerFromOrder()
                }
            )
        }

        registerCloseAction(BATCH_ORDER_ERROR_DIALOG_TAG) {
            closeActionFactory(
                positive = {
                    navigateToPickListItems()
                }
            )
        }

        registerCloseAction(GENERIC_RELOAD_DIALOG) {
            closeActionFactory(
                positive = {
                    load()
                }
            )
        }
        registerCloseAction(FIRST_LAUNCH_INTRO_DIALOG) {
            Timber.e("onGotItClicked [received]")
            updateFlagForFirstLaunch()
            closeActionFactory(
                dismiss = {

                }
            )
        }
    }

    private fun refreshTotalGamePoints() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { refreshTotalGamePoints() }
            } else {
                 // Call api to store all points
                 // Check for last api call 10 min refresh
                if (!apiCallTimeStamp.canMakeApiCall()){
                    isDataLoading.wrap { false }
                    return@launch
                }

                val result = apsRepo.getTotalGamesPoint()
                when (result) {
                    is ApiResult.Success -> {

                        result.data.totalPoints?.let {
                            apiCallTimeStamp.setPoints(it.toString())
                        }

                    }
                    is ApiResult.Failure -> {
                        if (apiCallTimeStamp.getPoints().isEmpty()){
                            handleApiError(result, retryAction = { refreshTotalGamePoints() })
                        }else{
                            // Do nothing
                        }

                    }
                }.exhaustive
            }
        }
    }

    fun runCardDataActions() {
        if (cardData.value?.isPrePickOrAdvancePick == true) {
            dueDay.value = ChronoUnit.DAYS.between(
                ZonedDateTime.now().toLocalDate(), (cardData.value?.expectedEndTime ?: ZonedDateTime.now()).toSameZoneInstantLocalDate()
            )
        }
        hideTimer.value = dueDay.value != 0L
        checkCountdownTimerData()
        selectedPickListActivityId = cardData.value?.actId?.toString()
        checkCustomerArrivalData()

        // Observe config flag flow for code unavailable enabled
        setNextPickTitle(cardData.value, AcuPickConfig.isCodeUnavailableEnabled())
        // clear the stale cache
        idRepository.clear()
    }

    private fun checkCustomerArrivalData() {
        if (cardData.value?.fulfillment?.toFulfillmentTypeUI() == FulfillmentTypeUI.DUG &&
            (cardData.value?.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED || cardData.value?.customerArrivalStatusUI == CustomerArrivalStatusUI.ARRIVED_NOT_STARTED) &&
            (cardData.value?.feScreenStatus == null || cardData.value?.feScreenStatus == FE_SCREEN_STATUS_STORE_NOTIFIED)
        ) {
            cardData.value?.customerArrivalTime?.let { customerArrivalTime -> startTimer(customerArrivalTime) }
            return
        }
        if (cardData.value?.isOrderReadyToPickUp == true) {
            when (cardData.value?.customerArrivalStatusUI) {
                CustomerArrivalStatusUI.ARRIVED ->
                    if (cardData.value?.fulfillment?.toFulfillmentTypeUI() == FulfillmentTypeUI.THREEPL)
                        cardData.value?.customerArrivalTime?.let { customerArrivalTime -> startTimer(customerArrivalTime) }

                CustomerArrivalStatusUI.ARRIVING, CustomerArrivalStatusUI.EN_ROUTE ->
                    cardData.value?.customerArrivalTime?.let { eta -> startTimer(eta, true) }

                else -> waitTimeSeconds.postValue(ARRIVING_SOON_WAIT_TIME_PLACEHOLDER)
            }
        }
    }

    private fun checkCountdownTimerData() {
        if (orderType.value.shouldShowCountdownTimer() && cardData.value?.isOrderReadyToPickUp == false) {
            startCoundownTimer(cardData.value?.countDownTimer)
        } else {
            timerJob?.cancel()
            timerStillActive.postValue(false)
        }
    }

    @VisibleForTesting
    internal fun setNextPickTitle(cardData: HomeCardData?, isCodeUnavailableEnabled: Boolean = false) {
        nextPickTitle.postValue(
            when {
                cardData?.isOrderReadyToPickUp == true -> app.getString(R.string.home_customer_waiting)
                orderType.value == OrderType.FLASH -> app.getString(R.string.hello_again_flash)
                else -> {
                    when (cardData?.isAssignedToMe) {
                        true -> app.getString(R.string.hello_continue_pick)
                        false -> app.getString(R.string.hello_again_next_pick, if (isCodeUnavailableEnabled) " Acupick" else "")
                        null -> ""
                    }
                }
            }
        )
    }

    fun onChatClicked(orderNumber: String) {
        viewModelScope.launch {
            pickRepository.pickList.value?.actId.toString().logError(
                "Null Activity Id. HomeViewModel(onChatClicked)," +
                    " User Id-${userRepo.user.value?.userId}, storeId-${userRepo.user.value?.selectedStoreId}"
            )
            pickRepository.pickList.value?.let {
                _navigationEvent.postValue(
                    NavigationEvent.Directions(
                        NavGraphDirections.actionToPickListItemsFragment(
                            activityId = it.actId.toString(), orderNumber = orderNumber,
                            navigateToChat = true
                        )
                    )
                )
            }
        }
    }

    private fun unassignPickerFromOrder() {
        viewModelScope.launch(dispatcherProvider.IO) {
            val cancelHandoffReq =
                CancelHandoffRequestDto(
                    cancelReasonCode = CancelReasonCode.WRONG_HANDOFF,
                    erId = cardData.value?.erId ?: 0L,
                    siteId = userRepo.user.value?.selectedStoreId
                )
            val result = isBlockingUi.wrap { apsRepo.cancelHandoff(cancelHandoffReq) }
            when (result) {
                is ApiResult.Success -> {
                    load(true)
                }
                is ApiResult.Failure -> {
                    handleApiError(result, retryAction = { unassignPickerFromOrder() })
                }
            }.exhaustive
        }
    }

    fun load(isRefresh: Boolean = false, isSwipeRefresh: Boolean = false) {
        timerJob?.cancel()
        intervalTimer?.cancel()
        if (isSwipeRefresh && autoInitiated) {
            loadingState.postValue(HomeLoadingState.Initial)
        }
        loadEvent.value = isRefresh

    }

    private val loadEvent = MutableStateFlow<Boolean?>(null).apply {
        filterNotNull().throttleFirst(THROTTLE_TIMEOUT_MS).onEach { loadHomeData(it) }.launchIn(viewModelScope)
    }

    private fun getCountAutoInitiate() =
        loadingState.value?.counter.getOrZero()

    private fun loadHomeData(isRefresh: Boolean = false) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val user = userRepo.user.value ?: run {
                acuPickLogger.w("[loadHomeData] user null - bypassing function execution")
                return@launch
            }
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { loadHomeData() }

                pickRepository.pickList.first()?.let { pickListInProgress ->
                    // Use this pick list to update UI
                    cardData.postValue(HomeCardData(user.userId, pickListInProgress))
                }
            } else {
                isDataLoading.wrap {
                    val result = (if (isRefresh) isDataRefreshing else isDataLoading)
                        .wrap {
                            completeHandoffUseCase()
                            completeHandoff1PLUseCase()
                            apsRepo.getAppSummary(siteId = user.selectedStoreId ?: "", userId = user.userId, counter = getCountAutoInitiate())
                        }

                    when (result) {
                        is ApiResult.Success -> {
                            val handOffOrders = result.data.orderCountByStore?.find { it.type == OrderByCountType.HAND_OFFS }
                            val openOrders = result.data.orderCountByStore?.find { it.type == OrderByCountType.PENDING_TO_STAGE }
                            result.data.activity?.let {
                                pickListType.postValue(it.getPickListType())
                            }
                            orderCount.postValue(result.data.activity?.getOrderCount())
                            orderType.postValue(result.data.activity?.orderType)
                            releaseTime.postValue(result.data.activity?.releasedEventDateTime)
                            showTimerPastDue.postValue(result.data.activity?.orderType.showTimerPastDue())
                            if (result.data.activity == null) {
                                associateCta.postValue(app.getString(R.string.there_is_no_task_right_now))
                                cardData.postValue(null)
                            }
                            openOrderCount.postValue(openOrders?.count?.toString() ?: "0")
                            handOffOrderCount.postValue(handOffOrders?.count?.toString() ?: "0")
                            dugCount.postValue(handOffOrders?.countFulfillmentTypes?.find { it.fulfilmentType == FulfillmentType.DUG }?.count?.toString() ?: "0")
                            deliveryCount.postValue(handOffOrders?.countFulfillmentTypes?.find { it.fulfilmentType == FulfillmentType.DELIVERY }?.count?.toString() ?: "0")

                            updateAutoInitiatedLoadingState(result.data.isAutoInitiated, result.data.activity)

                            val activity = result.data.activity ?: return@wrap

                            val isOrderAssinedToMeAndNotInStaging = result.data.activity?.assignedTo?.userId == user.userId && result.data.activity?.actType != ActivityType.DROP_OFF
                            Timber.d("Home activityDetail $isOrderAssinedToMeAndNotInStaging")
                            val activityDetail = if (isOrderAssinedToMeAndNotInStaging) {
                                isDataLoading.wrap {
                                    pickRepository.getActivityDetails(
                                        id = (
                                            if (activity.status == ActivityStatus.NEW && activity.actType == ActivityType.DROP_OFF)
                                                activity.prevActivityId
                                            else
                                                activity.actId
                                            ).toString().also {
                                            it.logError(
                                                "Null Activity Id. HomeViewModel(loadHomeData)," +
                                                    " Order Id-${activity.customerOrderNumber}, User Id-${user.userId}, storeId-${user.selectedStoreId}"
                                            )
                                        },
                                        true
                                    )
                                }
                            } else {
                                isDataLoading.wrap {
                                    pickRepository.getActivityDetails(
                                        id = (
                                            if (activity.status == ActivityStatus.NEW && activity.actType == ActivityType.DROP_OFF) activity.prevActivityId
                                            else activity.actId
                                            ).toString().also {
                                            it.logError(
                                                "Null Activity Id. HomeViewModel(loadHomeData)," +
                                                    " Order Id-${activity.customerOrderNumber}, User Id-${user.userId}, storeId-${user.selectedStoreId}"
                                            )
                                        },
                                        false
                                    )
                                }
                            }

                            updateCustomerType(activityDetail)
                            if (activity.getPickListType() == PickListBatchingType.Batch) {
                                when (activityDetail) {
                                    is ApiResult.Success -> {
                                        // Find all unique fulfillment types and convert them to FulfillmentTypeUI
                                        val allFulfillmentTypes = activityDetail.data.fulfillmentTypes().mapNotNull { it.toFulfillmentTypeUI() }.toSet()
                                        Timber.v("[loadHomeData] allFulfillmentTypes=$allFulfillmentTypes")
                                        activityFulfillmentTypes.postValue(allFulfillmentTypes)

                                        // FIXME Remove this to just rely on {result.data.activity?.routeVanNumber} when api is fixed not to return null for that field
                                        val vanNumber = result.data.activity?.routeVanNumber ?: activityDetail.data.itemActivities?.firstOrNull()?.routeVanNumber

                                        this@HomeViewModel.vanNumber.postValue(vanNumber)
                                    }
                                    is ApiResult.Failure -> handleApiError(activityDetail, retryAction = { loadHomeData() })
                                }
                            } else {
                                val fulfillmentTypes = result.data.activity?.fulfillment?.toFulfillmentTypeUI()?.let { setOf(it) } ?: emptySet()
                                activityFulfillmentTypes.postValue(fulfillmentTypes)
                                vanNumber.postValue(result.data.activity?.routeVanNumber)
                            }
                            // if activity is pickup/handoff, make activity details API call, and see if first container is a bag or tote
                            if (activity.actType == ActivityType.THREEPL_PICKUP || activity.actType == ActivityType.PICKUP && activity.isMultiSource == true) {
                                if (activityDetail is ApiResult.Success) {
                                    isOrderMfc.postValue(activityDetail.data.containerActivities?.firstOrNull()?.type == ContainerType.TOTE)
                                }
                            }

                            result.data.activity?.let { activity ->
                                val data = HomeCardData(user.userId, activity)
                                cardData.postValue(data)

                                pickRepository.pickList.value?.orderChatDetails?.map {
                                    async {
                                        val sid = conversationsRepository.getConversationId(it.customerOrderNumber.orEmpty())
                                        if (sid.isNotNullOrEmpty()) {
                                            conversationsRepository.insertOrUpdateConversation(sid)
                                        }
                                    }
                                }?.awaitAll() ?: run { conversationsRepository.clear() }
                            }
                        }
                        is ApiResult.Failure -> handleApiError(result, retryAction = { load() })
                    }.exhaustive
                }
                isDataRefreshing.postValue(false)

                // Reset loadEvent so the StateFlow won't conflate multiple true requests together.
                loadEvent.value = null
            }
        }
    }

    private fun updateAutoInitiatedLoadingState(
        isAutoInitiated: Boolean?,
        activity: ActivityAndErDto?,
    ) {
        if (autoInitiated) {
            when {
                activity != null -> {
                    stopTimer()
                    loadingState.postValue(HomeLoadingState.End)
                }
                isAutoInitiated == true && loadingState.value == HomeLoadingState.Initial -> {
                    startTimer { loadingState.postValue(HomeLoadingState.Intermediate) }
                }
                else -> loadingState.postValue(HomeLoadingState.End)
            }
        } else loadingState.postValue(HomeLoadingState.End)
    }

    private fun startTimer(onComplete: () -> Unit) {
        stopTimer()
        intervalTimer = startIntervalRefreshTimer(onComplete)
    }

    private fun stopTimer() = intervalTimer?.cancel()

    private fun startIntervalRefreshTimer(onComplete: () -> Unit) = viewModelScope.launch {
        delay(AWAIT_TIME_INTERVAL_PRE_PICK)
        loadHomeData(true)
        onComplete()
    }

    private fun navigateToArrivalsScreen() {
        activityViewModel.is1Pl.postValue(true)
        activityViewModel.orderNumberFor1PlToSelect.postValue(cardData.value?.customerOrderNumber?.toLongOrNull())
        _navigationEvent.postValue(
            NavigationEvent.Action(
                R.id.action_to_arrivalsPagerFragment
            )
        )
    }

    fun onPickClicked() {
        if (!selectedPickListActivityId.isValidActivityId()) {
            inlineDialogEvent.postValue(
                CustomDialogArgDataAndTag(
                    data = RELOAD_DILAOG,
                    tag = GENERIC_RELOAD_DIALOG
                )
            )
            return
        }

        when {
            cardData.value?.is1Pl == true -> navigateToArrivalsScreen()
            cardData.value?.isOrderReadyToPickUp == true -> onBeginHandoffClicked()
            else -> onPickListCardCtaClicked()
        }
    }

    private fun begin1PlHandOff() {
        viewModelScope.launch {
            val (actId, vanId) = cardData.value?.let { it.actId to it.vanNumber } ?: return@launch
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { begin1PlHandOff() }
            } else {
                val handOffResult = isBlockingUi.wrap { assign1PlUserToHandOffs(actId, vanId) }
                when (handOffResult) {
                    is ApiResult.Success -> {
                        navigateToArrivalsScreen()
                    }
                    is ApiResult.Failure -> {
                        handle1PlResultFailure(handOffResult) { begin1PlHandOff() }
                    }
                }
            }
        }
    }

    fun onPickListCardCtaClicked() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (cardData.value?.isOrderInStagePhase == true) {
                navigateToStaging()
            } else {
                if (networkAvailabilityManager.isConnected.first().not()) {
                    networkAvailabilityManager.triggerOfflineError { onPickListCardCtaClicked() }
                } else {
                    // Always try to assign pick list to myself just in case someone took it (ACUPICK-1313)
                    when (val result = isBlockingUi.wrap { assignPickToMe() }) {
                        is ApiResult.Success -> {
                            navigateToPickListItems()
                        }
                        is ApiResult.Failure -> {
                            if (result is ApiResult.Failure.Server) {
                                val type = result.error?.errorCode?.resolvedType
                                when (type?.cannotAssignToOrder()) {
                                    true -> {
                                        val serverErrorType =
                                            if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.PICKLIST else CannotAssignToOrderDialogTypes.REGULAR
                                        serverErrorCannotAssignUser(serverErrorType, isBatchOrder.value == true)
                                    }
                                    else -> handleApiError(result, retryAction = { onPickListCardCtaClicked() })
                                }
                            } else {
                                handleApiError(result, retryAction = { onPickListCardCtaClicked() })
                            }
                        }
                    }.exhaustive
                }
            }
        }
    }

    private fun showOfAgeVerificationDialog() {
        inlineDialogEvent.postValue(
            CustomDialogArgDataAndTag(
                data = OF_AGE_ASSOCIATE_VERIFICATION_DATA,
                tag = DestageOrderPagerViewModel.OF_AGE_ASSOCIATE_VERIFICATION_TAG,
            )
        )
    }

    fun onBeginHandoffClicked() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError {
                    onBeginHandoffClicked()
                }
            } else {

                when (val result = isBlockingUi.wrap { assignUserToHandOffs() }) {
                    is ApiResult.Success -> {

                        val detailResult = result.data.first().erId?.let {
                            apsRepo.pickUpActivityDetails(it, true)
                        }

                        when (detailResult) {
                            is ApiResult.Success -> {
                                if (siteRepository.isCctEnabled) {
                                    // precache image urls
                                    val imageUrls = mutableListOf<String?>()
                                    detailResult.data.orderSummary?.forEach {
                                        imageUrls.add(it?.imageUrl)
                                        if (it?.substitutedWith.isNotNullOrEmpty()) {
                                            it?.substitutedWith?.forEach { subSummary ->
                                                imageUrls.add(subSummary?.imageUrl)
                                            }
                                        }
                                    }
                                    imagePreCacher.preCacheImages(imageUrls.mapNotNull { getSizedImageUrl(it, ImageSizePreset.ItemDetails) })
                                }
                                if (siteRepository.isDigitizeAgeVerificationEnabled) {
                                    isBlockingUi.wrap {
                                        checkRegulated(detailResult)
                                    }
                                } else {
                                    handOffResult.value = detailResult.data
                                    navigateToPickup(detailResult.data)
                                }
                            }
                            is ApiResult.Failure -> {
                                acuPickLogger.e("onBeginHandoffClicked pickUpActivityDetails: $detailResult")
                                handleResultFailure(detailResult) { onBeginHandoffClicked() }
                            }

                            else -> {}
                        }
                    }
                    is ApiResult.Failure -> {
                        acuPickLogger.e("onBeginHandoffClicked assignUserToHandOffs: $result")
                        handleResultFailure(result) { onBeginHandoffClicked() }
                    }
                }
            }
        }
    }

    private fun handleResultFailure(result: ApiResult.Failure, retryAction: () -> Unit) {
        if (result is ApiResult.Failure.Server) {
            val type = result.error?.errorCode?.resolvedType
            when (type?.cannotAssignToOrder()) {
                true -> {
                    val serverErrorType =
                        if (type == ServerErrorCode.CANNOT_ASSIGN_COMPLETED_ACTIVITY) CannotAssignToOrderDialogTypes.HANDOFF else CannotAssignToOrderDialogTypes.REGULAR
                    serverErrorCannotAssignUser(serverErrorType, isBatchOrder.value == true)
                }
                else -> handleApiError(result, retryAction = { retryAction() })
            }
        } else {
            handleApiError(result, retryAction = { retryAction() })
        }
    }

    private fun handle1PlResultFailure(result: ApiResult<List<ActivityDto>>, retryAction: () -> Unit) {
        if (result is ApiResult.Failure.Server) {
            val type = result.error?.errorCode?.resolvedType
            when (type?.cannotAssignToOrder()) {
                true -> {
                    val serverErrorType =
                        if (type == ServerErrorCode.NO_OVER_RIDE_FLAG) CannotAssignToOrderDialogTypes.HANDOFF_REASSIGN else CannotAssignToOrderDialogTypes.REGULAR
                    serverErrorCannotAssignUser(serverErrorType, isBatchOrder.value == true)
                }
                else -> handleApiError(result, retryAction = { retryAction() })
            }
        } else {
            handleApiError(result as ApiResult.Failure, retryAction = { retryAction() })
        }
    }

    private suspend fun updateCustomerType(activityDetail: ApiResult<ActivityDto>) {
        when (activityDetail) {
            is ApiResult.Success -> {
                isSnap.emit(activityDetail.data.isSnap.orFalse())
                isSubscription.emit(activityDetail.data.isSubscription.orFalse())
            }
            is ApiResult.Failure -> {
                handleApiError(activityDetail, retryAction = { loadHomeData() })
            }
        }
    }

    private fun checkRegulated(result: ApiResult<ActivityDto>?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first().not()) {
                networkAvailabilityManager.triggerOfflineError { checkRegulated(result) }
            } else {
                when (result) {
                    is ApiResult.Success -> {
                        if (regulatedItemCheck(result.data) == true) {
                            handOffResult.value = result.data
                            showOfAgeVerificationDialog()
                        } else {
                            navigateToPickup(result.data)
                        }
                    }
                    is ApiResult.Failure -> handleResultFailure(result) { checkRegulated(result) }
                    else -> {}
                }
            }
        }
    }

    private fun regulatedItemCheck(data: ActivityDto) = data.containerItems?.any { erItemDto ->
        erItemDto.pickedUpcCodes?.any { pickedItemUpcDto ->
            pickedItemUpcDto.regulated == true
        } == true
    }

    private suspend fun assignPickToMe() =
        pickRepository.assignUser(
            AssignUserRequestDto(
                actId = selectedPickListActivityId?.toLong() ?: 0,
                replaceOverride = false,
                user = userRepo.user.value?.toUserDto(),
                defaultPickListSelected = true,
                tokenizedLdapId = userRepo.user.value?.tokenizedLdapId
            )
        )

    private suspend fun assignUserToHandOffs() =
        apsRepo.assignUserToHandoffs(
            AssignUserWrapperRequestDto(
                actIds = listOf<Long>(selectedPickListActivityId?.toLong() ?: 0),
                replaceOverride = false,
                user = userRepo.user.value?.toUserDto(),
                resetPickList = false,
                etaArrivalFlag = siteRepository.isEtaArrivalEnabled
            )
        )

    private suspend fun assign1PlUserToHandOffs(actId: Long?, vanId: String?) =
        apsRepo.get1PLTruckRemovalList(
            Get1PLTruckRemovalItemListRequestDto(
                activityId = actId,
                replaceOverride = false,
                resetPickList = true,
                user = userRepo.user.value?.toUserDto(),
                vanId = vanId,
                siteId = userRepo.user.value?.selectedStoreId,
                eventTimeStamp = ZonedDateTime.now()
            )
        )

    private fun startTimer(startTime: ZonedDateTime, countUp: Boolean = false) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            flow {
                do {
                    if (countUp) {
                        val seconds = ChronoUnit.SECONDS.between(ZonedDateTime.now(), startTime)
                        if (seconds < 0) emit(ARRIVING_SOON_WAIT_TIME_PLACEHOLDER)
                        else emit(seconds)
                    } else emit(ChronoUnit.SECONDS.between(startTime, ZonedDateTime.now()))
                    delay(1000)
                } while (true)
            }.collect {
                waitTimeSeconds.postValue(it)
            }
        }
    }

    private fun startCoundownTimer(startTime: String?) {
        val stagingTime = startTime?.toZoneTime() ?: ZonedDateTime.now()
        timerStillActive.postValue(true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            flow {
                while (stagingTime > ZonedDateTime.now()) {
                    emit(ChronoUnit.MILLIS.between(stagingTime, ZonedDateTime.now()))
                    delay(80)
                    timeSinceOrderReleasedMs = System.currentTimeMillis().minus(releaseTime.value?.toInstant()?.toEpochMilli() ?: 0)
                }
                timerStillActive.postValue(false)
            }.collect {
                countdownDurationMs.postValue(abs(it))
                setTimerColor()
            }
        }
    }

    fun setTimerColor() {
        when {
            (countdownDurationMs.value ?: 0) < warningTimeMs -> timerColor.postValue(R.color.semiDarkRed)
            timeSinceOrderReleasedMs > concernTimeMs -> timerColor.postValue(R.color.semiLightOrange)
            else -> timerColor.postValue(R.color.grey_700)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Navigation
    // /////////////////////////////////////////////////////////////////////////
    private fun navigateToPickListItems() {
        _navigationEvent.postValue(
            NavigationEvent.Directions(
                HomeFragmentDirections.actionToPickListItemsFragment(
                    activityId = selectedPickListActivityId ?: "",
                    toteEstimate = cardData.value?.toteEstimate
                )
            )
        )
    }

    private fun navigateToStaging() {

        if (cardData.value?.isWineOrder.orFalse()) {
            cardData.value?.actId?.let {
                val savedData = wineStagingStateRepo.loadStagingPartOne(cardData.value?.customerOrderNumber.orEmpty())
                Timber.d("[saveStagingOne] key2: $savedData")

                val di = when (savedData?.nextActivityId) {
                    WineStagingType.WineStaging1 -> {
                        HomeFragmentDirections.actionToWineStagingFragment(
                            wineStagingParams = WineStagingParams(
                                activityId = savedData.activityId.toString(),
                                contactName = savedData.contactName.orEmpty(),
                                customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                                shortOrderNumber = savedData.shorOrderId.toString(),
                                entityId = savedData.entityId.toString(),
                                stageByTime = savedData.stageByTime.orEmpty(),
                                pickedUpBottleCount = savedData.bottleCount?.toString() ?: "0",
                            )
                        )
                    }
                    WineStagingType.WineStaging2 -> {
                        HomeFragmentDirections.actionToWineStaging2Fragment(
                            wineStagingParams = WineStagingParams(
                                activityId = savedData.activityId.toString(),
                                contactName = savedData.contactName.orEmpty(),
                                customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                                shortOrderNumber = savedData.shorOrderId.toString(),
                                entityId = savedData.entityId.toString(),
                                stageByTime = savedData.stageByTime.orEmpty(),
                                pickedUpBottleCount = "",
                            )
                        )
                    }
                    WineStagingType.WineStaging3 -> {
                        HomeFragmentDirections.actionToWineStaging3Fragment(
                            wineStagingParams = WineStagingParams(
                                activityId = savedData.activityId.toString(),
                                contactName = savedData.contactName.orEmpty(),
                                customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                                shortOrderNumber = savedData.shorOrderId.toString(),
                                entityId = savedData.entityId.toString(),
                                stageByTime = savedData.stageByTime.orEmpty(),
                                pickedUpBottleCount = "",
                            ),
                            boxUiData = savedData.boxInfo?.let { BoxUiData(it) }
                        )
                    }
                    else -> { // case when cache is cleared
                        HomeFragmentDirections.actionToWineStagingFragment(
                            wineStagingParams = WineStagingParams(
                                activityId = cardData.value?.prevActivityId.toString(),
                                contactName = cardData.value?.contactNameForWineShipping.orEmpty(),
                                customerOrderNumber = cardData.value?.customerOrderNumber.orEmpty(),
                                shortOrderNumber = cardData.value?.shortOrderNumber.toString(),
                                entityId = cardData.value?.entityReference.toString(),
                                stageByTime = cardData.value?.stageByTime.orEmpty(),
                                pickedUpBottleCount = cardData.value?.itemQty?.toString().orEmpty()
                            )
                        )
                    }
                }
                _navigationEvent.postValue(NavigationEvent.Directions(di))
            }
        } else {
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    HomeFragmentDirections.actionHomeFragmentToStagingFragment(
                        activityId = cardData.value?.prevActivityId.toString(),
                        isPreviousPrintSuccessful = true,
                        shouldClearData = false
                    )
                )
            )
        }
    }

    private fun navigateToPickup(activityDto: ActivityDto) {
        viewModelScope.launch {
            Timber.d("pickRepository customerOrderNumber ${pickRepository.pickList.value?.customerOrderNumber}")
            _navigationEvent.postValue(
                NavigationEvent.Directions(
                    HomeFragmentDirections.actionHomeFragmentToArrivalsResultDetailsFragment(
                        SelectedActivities(
                            arrayListOf(
                                activityDto.copy(
                                    nextActExpStartTime = activityDto.nextActExpStartTime ?: ZonedDateTime.now()
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    private fun getIllustrationDrawable(
        type: OrderType,
        card: HomeCardData?,
        fulFillmentTypes: Set<FulfillmentTypeUI>,
        isCattEnabled: Boolean,
        customerTypeIcon: CustomerType?,
    ) = if (card?.is1Pl == true) {
        R.drawable.ic_one_pl_truck
    } else if (card?.isOrderReadyToPickUp == true) {
        when {
            fulFillmentTypes.contains(FulfillmentTypeUI.DUG) -> R.drawable.ic_handoff_dug
            fulFillmentTypes.contains(FulfillmentTypeUI.ONEPL) ||
                fulFillmentTypes.contains(FulfillmentTypeUI.THREEPL) -> R.drawable.ic_handoff_3pl

            else -> null
        }
    } else if (card?.isOrderInStagePhase == true) {
        R.drawable.ic_staging
    } else {
        when {
            customerTypeIcon != null -> getCustomerTypeDrawable(isCattEnabled, customerTypeIcon)
            isBatchOrder.value == true -> R.drawable.ic_batchpicking
            type == OrderType.REGULAR || type == OrderType.EXPRESS -> R.drawable.ic_standard_picking
            type == OrderType.FLASH || type == OrderType.FLASH3P -> R.drawable.ic_flashpicking
            else -> null
        }
    }

    private fun getCustomerTypeDrawable(isCattEnabled: Boolean, customerTypeIcon: CustomerType): Int {
        return when (customerTypeIcon) {
            CustomerType.SNAP -> if (isCattEnabled.orFalse()) R.drawable.ic_home_ebt_order else R.drawable.ic_home_ebt_order
            CustomerType.SUBSCRIPTION -> R.drawable.ic_home_freshpass_order
            CustomerType.BOTH -> R.drawable.ic_home_ebt_order
        }
    }

    companion object {
        // Throttle outside access to loading data.
        private const val THROTTLE_TIMEOUT_MS = 200L

        /** Value that denotes arriving soon wait time */
        const val ARRIVING_SOON_WAIT_TIME_PLACEHOLDER = -1L

        const val AWAIT_TIME_INTERVAL_PRE_PICK = 10000L

        const val FIRST_LAUNCH_INTRO_DIALOG = "FIRST_LAUNCH_INTRO_DIALOG"
    }


    private fun showFirstLaunchDialog() {
        viewModelScope.launch {
            /*if (apiCallTimeStamp.isFirstTimeLaunch()) {
                return@launch
            }*/
            val data = CustomDialogArgData(
                title = StringIdHelper.Raw(""),
                positiveButtonText = StringIdHelper.Id(R.string.ok),
                cancelOnTouchOutside = false,
                dialogType = DialogType.FirstLaunchDialogFragment
            )
            inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data, FIRST_LAUNCH_INTRO_DIALOG))
        }
    }
    private fun updateFlagForFirstLaunch() = viewModelScope.launch{
        Timber.e("onGotItClicked [received]")
        apiCallTimeStamp.updateFirstLaunchStatus(true)
    }
}

private fun OrderType?.showTimerPastDue(): Boolean =
    this == OrderType.FLASH || this == OrderType.FLASH3P
