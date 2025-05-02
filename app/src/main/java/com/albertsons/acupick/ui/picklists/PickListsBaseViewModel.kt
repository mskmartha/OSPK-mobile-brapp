package com.albertsons.acupick.ui.picklists

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.BuildConfig
import com.albertsons.acupick.NavGraphDirections
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.ToteEstimate
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.ReassignDropOffRequestDto
import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.response.ActivityAndErDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ActivityDtoByCategoryDto
import com.albertsons.acupick.data.model.response.WineStagingType
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.isWineOrder
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.data.repository.WineShippingStageStateRepository
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.navigation.NavigationEvent
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.home.HomeFragmentDirections
import com.albertsons.acupick.ui.picklists.open.OrderCategoryUi
import com.albertsons.acupick.ui.staging.winestaging.BoxUiData
import com.albertsons.acupick.ui.staging.winestaging.WineStagingParams
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

abstract class PickListsBaseViewModel(
    app: Application,
    protected val activityViewModel: MainActivityViewModel,
) : BaseViewModel(app) {
    // DI
    protected val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    protected val dispatcherProvider: DispatcherProvider by inject()
    protected val userRepo: UserRepository by inject()
    protected val pickRepository: PickRepository by inject()
    protected val apsRepo: ApsRepository by inject()
    protected val siteRepository: SiteRepository by inject()
    protected val conversationsRepository: ConversationsRepository by inject()
    private val wineStagingStateRepo: WineShippingStageStateRepository by inject()

    // Data
    val pickLists: LiveData<List<ActivityAndErDto>> = MutableLiveData()
    val selectedPickListActivityId: LiveData<String> = MutableLiveData()
    val selectedPickListToteEstimate: LiveData<ToteEstimate?> = MutableLiveData()
    protected var pickAssigned: ActivityAndErDto? = null

    // UI
    val isDataLoading: LiveData<Boolean> = MutableLiveData(true)
    val isSpinnerShowing: LiveData<Boolean> = MutableLiveData(false)
    val isDataRefreshing: LiveData<Boolean> = MutableLiveData(false)
    val isMfcSite: LiveData<Boolean> = MutableLiveData(false)
    val isFlashOrderEnabled = liveData { emit(siteRepository.isFlashOrderEnabled) }
    val isWineOrder = MutableLiveData<Boolean>(false)
    val noPicks = MutableLiveData(true)
    val isInternalBuild = MutableLiveData(BuildConfig.DEBUG || BuildConfig.INTERNAL)
    val isSkeletonStateShowing = combine(isDataLoading.asFlow(), isDataRefreshing.asFlow()) { loading, refreshing ->
        loading && refreshing.not()
    }.asLiveData()
    val acknowledgedFlashOrderActId: LiveData<Long> = if (siteRepository.isFlashInterjectionEnabled) activityViewModel.acknowledgedPickerDetails.map { it.actId ?: 0 } else MutableLiveData()
    val searchQuery = MutableStateFlow("")
    private val searchQueryDebounced = MutableStateFlow("")

    private val selectedOrderCategory: LiveData<OrderCategoryUi> = MutableLiveData(OrderCategoryUi.ALL)

    val picklistCategories = pickLists.map { orders ->
        mutableMapOf(OrderCategoryUi.ALL to orders).apply {
            putIfNotEmpty(orders.filter { it.orderType == OrderType.FLASH }) { put(OrderCategoryUi.FLASH, it) }
            putIfNotEmpty(orders.filter { it.is3p == true }) { put(OrderCategoryUi.PARTNER_PICK, it) }
            putIfNotEmpty(orders.filter { it.orderType == OrderType.EXPRESS }) { put(OrderCategoryUi.EXPRESS, it) }
            putIfNotEmpty(orders.filter { it.orderType == OrderType.REGULAR }) { put(OrderCategoryUi.REGULAR, it) }
        }.toMap()
    }

    val filteredPickList = combine(picklistCategories.asFlow(), selectedOrderCategory.asFlow(), searchQueryDebounced) { pickList, orderCategory, searchQueryDebounced ->
        if (searchQueryDebounced.isNotNullOrEmpty()) {
            val data = pickLists.value?.filter { it.customerOrderNumber?.contains(searchQueryDebounced) == true }
            data.orEmpty()
        } else {
            pickList[orderCategory].orEmpty()
        }
    }.asLiveData()

    private fun putIfNotEmpty(activities: List<ActivityAndErDto>, add: (activities: List<ActivityAndErDto>) -> Unit) {
        if (activities.isNotEmpty()) add(activities)
    }

    init {
        pickLists.observeForever {
            isWineOrder.value = it.isNotNullOrEmpty() && it[0].isWineOrder() // all wine orders
            noPicks.postValue(it.isNullOrEmpty())
        }

        viewModelScope.launch {
            searchQuery
                .debounce(1000) // debounce for 1 seconds
                .collect { query ->
                    searchQueryDebounced.value = query
                }
        }
    }

    // Abstracts for unique required child class logic
    abstract fun loadData(isRefresh: Boolean = false)
    abstract fun onPickClicked(pickList: ActivityAndErDto)
    abstract fun transferPick()

    // Todo use to set tabs mutable once Open/Team PickList has been converted to new Pager style
    abstract val categoryStatus: CategoryStatus
    abstract val tagList: List<String>

    // Common logic for Load Data
    fun loadPickData(open: Boolean, isRefresh: Boolean = false, filter: (List<ActivityDtoByCategoryDto>?) -> ActivityDtoByCategoryDto?) {
        viewModelScope.launch(dispatcherProvider.IO) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> {
                    isMfcSite.postValue(siteRepository.isMFCSite)

                    val user = userRepo.user.value ?: run {
                        acuPickLogger.w("[loadData] user null - bypassing function execution")
                        return@launch
                    }

                    val result = (if (isRefresh) isDataRefreshing else isDataLoading).wrap {
                        apsRepo.searchActivities(
                            siteId = user.selectedStoreId ?: "",
                            userId = user.userId,
                            assignedToMe = true,
                            assigned = true,
                            open = open,
                            pickUpReady = false,
                            stageByTime = null,
                            hideFresh = true // this flag is always be true
                        )
                    }

                    when (result) {
                        is ApiResult.Success -> {
                            pickAssigned = result.data.find { it.category == CategoryStatus.ASSIGNED_TO_ME }?.data?.firstOrNull { activityAndErDto ->
                                activityAndErDto.status == ActivityStatus.RELEASED || activityAndErDto.status == ActivityStatus.IN_PROGRESS || activityAndErDto.status == ActivityStatus.NEW
                            }
                            pickLists.postValue(filter(result.data)?.data.orEmpty())
                        }
                        is ApiResult.Failure -> handleApiError(result, retryAction = { loadData() })
                    }.exhaustive
                }
                false -> {
                    networkAvailabilityManager.triggerOfflineError { loadData() }
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Common functions
    // /////////////////////////////////////////////////////////////////////////

    fun transferPickToMe() {
        viewModelScope.launch(dispatcherProvider.Main) {
            when (networkAvailabilityManager.isConnected.first()) {
                true -> transferPick()
                false -> networkAvailabilityManager.triggerOfflineError { transferPickToMe() }
            }.exhaustive
        }
    }

    protected suspend fun assignPickToMe(replaceOverride: Boolean, isFromTeamPickList: Boolean = false): ApiResult<ActivityDto> {
        val pickPosition = pickLists.value?.indexOfFirst { it.actId == selectedPickListActivityId.value?.toLong() ?: 0 }
        // When called from TeamPickListsViewModel, defaultPickListSelected should always be false
        val isFirstPickList = if (isFromTeamPickList) {
            false
        } else {
            pickPosition == 0
        }
        val temp = AssignUserRequestDto(
            actId = selectedPickListActivityId.value?.toLong() ?: 0,
            replaceOverride = replaceOverride,
            defaultPickListSelected = isFirstPickList,
            user = userRepo.user.value?.toUserDto(),
            tokenizedLdapId = userRepo.user.value?.tokenizedLdapId
        )
        return pickRepository.assignUser(temp)
    }

    protected suspend fun reAssignToMe(): ApiResult<Unit> {
        val reassign = ReassignDropOffRequestDto(
            actId = selectedPickListActivityId.value?.toLong() ?: 0L,
            user = UserDto(
                firstName = userRepo.user.value?.firstName,
                lastName = userRepo.user.value?.lastName,
                userId = userRepo.user.value?.userId
            )

        )
        return pickRepository.reAssignUserStaging(reassign)
    }

    protected fun navigateToStagingDirections(pickList: ActivityAndErDto? = null) = NavigationEvent.Directions(
        if (isWineOrder.value.orFalse()) {
            val savedData = wineStagingStateRepo.loadStagingPartOne(pickAssigned?.customerOrderNumber.orEmpty())
            when (savedData?.nextActivityId) {
                WineStagingType.WineStaging1 -> {
                    NavGraphDirections.actionToWineStagingFragment(
                        wineStagingParams = WineStagingParams(
                            contactName = savedData.contactName.orEmpty(),
                            shortOrderNumber = savedData.shorOrderId.orEmpty(),
                            customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                            stageByTime = savedData.stageByTime.orEmpty(),
                            activityId = savedData.activityId.toString(),
                            entityId = savedData.entityId.orEmpty(),
                            pickedUpBottleCount = savedData.bottleCount.toString()
                        )
                    )
                }
                WineStagingType.WineStaging2 -> {
                    HomeFragmentDirections.actionToWineStaging2Fragment(
                        wineStagingParams = WineStagingParams(
                            contactName = savedData.contactName.orEmpty(),
                            shortOrderNumber = savedData.shorOrderId.orEmpty(),
                            customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                            stageByTime = savedData.stageByTime.orEmpty(),
                            activityId = savedData.activityId.toString(),
                            entityId = savedData.entityId.orEmpty(),
                            pickedUpBottleCount = savedData.bottleCount.toString()
                        )
                    )
                }
                WineStagingType.WineStaging3 -> {
                    NavGraphDirections.actionToWineStaging3Fragment(
                        wineStagingParams = WineStagingParams(
                            contactName = savedData.contactName.orEmpty(),
                            shortOrderNumber = savedData.shorOrderId.orEmpty(),
                            customerOrderNumber = savedData.customerOrderNumber.orEmpty(),
                            stageByTime = savedData.stageByTime.orEmpty(),
                            activityId = savedData.activityId.toString(),
                            entityId = savedData.entityId.orEmpty(),
                            pickedUpBottleCount = savedData.bottleCount.toString()
                        ),
                        boxUiData = savedData.boxInfo?.let { BoxUiData(it) }
                    )
                }
                else -> { // from network
                    NavGraphDirections.actionToWineStagingFragment(
                        wineStagingParams = WineStagingParams(
                            contactName = pickList?.fullContactName().orEmpty(),
                            shortOrderNumber = pickList?.shortOrderNumber.orEmpty(),
                            customerOrderNumber = pickList?.customerOrderNumber.orEmpty(),
                            stageByTime = pickList?.stageByTime().orEmpty(),
                            activityId = pickList?.prevActivityId.toString(),
                            entityId = pickList?.entityReference?.entityId.orEmpty(),
                            pickedUpBottleCount = pickList?.itemQty?.toInt()?.toString().orEmpty()
                        )
                    )
                }
            }
        } else {
            NavGraphDirections.actionPickListFragmentToStagingFragment(
                activityId = pickAssigned?.prevActivityId.toString(),
                isPreviousPrintSuccessful = true,
                shouldClearData = false
            )
        }
    )

    /** Returns whether a pick list is completed, but did not have bag counts entered */
    fun isPickListCompletedButHasNotStartedStaging(pickList: ActivityAndErDto?) =
        pickList?.assignedTo?.userId == userRepo.user.value?.userId && pickList?.status == ActivityStatus.NEW

    fun onCategorySelected(orderCategoryUi: OrderCategoryUi) {
        selectedOrderCategory.postValue(orderCategoryUi)
    }
}
