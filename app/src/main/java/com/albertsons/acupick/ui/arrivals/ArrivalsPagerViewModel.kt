package com.albertsons.acupick.ui.arrivals

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ErOrderStatus
import com.albertsons.acupick.data.model.FulfillmentSubType
import com.albertsons.acupick.data.model.response.ErDto
import com.albertsons.acupick.data.model.response.OnePlDto
import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.data.repository.ApsRepository
import com.albertsons.acupick.data.repository.ArrivalsRepository
import com.albertsons.acupick.data.repository.IdRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.domain.extensions.throttleFirst
import com.albertsons.acupick.domain.extensions.throttleLatest
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.CustomDialogArgDataAndTag
import com.albertsons.acupick.ui.dialog.DialogType
import com.albertsons.acupick.ui.dialog.closeActionFactory
import com.albertsons.acupick.ui.models.ArrivalOrdersTabUI
import com.albertsons.acupick.ui.models.OrderItemUI
import com.albertsons.acupick.ui.util.DrawableIdHelper
import com.albertsons.acupick.ui.util.getOrZero
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.toIdHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject

const val MAX_ORDER_SELECTION = 3
const val MAX_ORDER_SELECTION_1PL = 1
const val UI_OUTPUT_THROTTLE_MILLIS = 500L
const val UI_INPUT_DEBOUNCE_MILLIS = 50L
const val THROTTLE_TIMEOUT_MS = 200L

class ArrivalsPagerViewModel(val app: Application, val activityViewModel: MainActivityViewModel) : BaseViewModel(app) {

    // DI
    private val apsRepo: ApsRepository by inject()
    private val idRepository: IdRepository by inject()
    private val dispatcherProvider: DispatcherProvider by inject()
    private val networkAvailabilityManager: NetworkAvailabilityManager by inject()
    private val siteRepository: SiteRepository by inject()
    private val userRepo: UserRepository by inject()
    private val arrivalRepo: ArrivalsRepository by inject()

    // UI Observables
    val results = MutableLiveData<List<OrderItemUI?>>(listOf())
    private val throttledResults = results.asFlow().throttleLatest(UI_OUTPUT_THROTTLE_MILLIS).asLiveData()
    val tabs: LiveData<List<ArrivalOrdersTabUI>> = throttledResults.map { resultList ->
        listOf(
            ArrivalOrdersTabUI(
                tabLabel = app.getString(R.string.pickup_ready_home),
                tabArgument = resultList,
            ),
            ArrivalOrdersTabUI(
                tabLabel = app.getString(R.string.in_progress_orders),
                tabArgument = resultList,
            )
        )
    }

    val isDataRefreshing: LiveData<Boolean> = MutableLiveData()
    val isDataLoading: LiveData<Boolean> = MutableLiveData()
    private val badgeArrivalsAction: LiveData<Boolean> = MutableLiveData()

    fun load(isRefresh: Boolean = false) {
        loadEvent.value = isRefresh
    }

    private val loadEvent = MutableStateFlow<Boolean?>(null).apply {
        if (value != null) {
            filterNotNull().throttleFirst(THROTTLE_TIMEOUT_MS).onEach { loadResults(it) }.launchIn(viewModelScope)
        }
    }

    init {
        idRepository.clear()
        clearToolbarEvent.postValue(Unit)
        checkForArrivedOrders()
        activityViewModel.is1Pl.observeForever(::switchNavigationIconAndText)
        badgeArrivalsAction.observeForever {
            switchNavigationIconAndText(activityViewModel.is1Pl.value.orFalse(), it)
        }
        setupNavIconClick()

        registerCloseAction(ONE_PL_DIALOG) {
            closeActionFactory(
                positive = {
                    arrivalRepo.showDialog()
                    loadResults(true)
                    switchNavigationIconAndText(activityViewModel.switchArrivalType())
                }
            )
        }
    }

    private fun setupNavIconClick() {
        viewModelScope.launch(dispatcherProvider.IO) {
            toolbarRightFirstImageEvent.asFlow().collect {
                if (activityViewModel.is1Pl.value == true || arrivalRepo.is1plDialogShown) {
                    switchNavigationIconAndText(activityViewModel.switchArrivalType())
                    loadResults(true)
                } else {
                    if (activityViewModel.is1Pl.value == false) inlineDialogEvent.postValue(CustomDialogArgDataAndTag(data = get1PlDialog(), ONE_PL_DIALOG))
                }
            }
        }
    }

    private fun switchNavigationIconAndText(is1pl: Boolean, isShowBadgeArrivals: Boolean? = badgeArrivalsAction.value) {
        if (siteRepository.isCas1PL) {
            val navIcon = if (is1pl && isShowBadgeArrivals == true) R.drawable.ic_vehicle_red_dot else if (is1pl) R.drawable.ic_1_pl else R.drawable.ic_delivery
            changeToolbarRightFirstExtraImageEvent.postValue(DrawableIdHelper.Id(navIcon))
        }
        val navText = if (is1pl) R.string.one_pl_arrivals else R.string.arrivals
        changeToolbarTitleEvent.postValue(app.getString(navText))
    }

    // /////////////////////////////////////////////////////////////////////////
    // API calls
    // /////////////////////////////////////////////////////////////////////////
    fun loadResults(isRefresh: Boolean = false) {
        viewModelScope.launch(dispatcherProvider.IO) {
            val user = userRepo.user.value ?: run {
                acuPickLogger.w("[loadHomeData] user null - bypassing function execution")
                return@launch
            }

            if (activityViewModel.is1Pl.value.orFalse()) {
                val result = (if (isRefresh) isDataRefreshing else isDataLoading)
                    .wrap { apsRepo.search1PlArrivalsOrders(siteId = user.selectedStoreId) }

                when (networkAvailabilityManager.isConnected.first()) {
                    true -> {
                        when (result) {
                            is ApiResult.Success<List<OnePlDto>> -> {
                                val resultList = result.data.map { OrderItemUI(it, user.selectedStoreId) }
                                results.postValue(resultList)
                            }
                            is ApiResult.Failure -> {
                                results.notifyObservers()
                                handleApiError(result, retryAction = { loadResults() })
                            }
                        }.exhaustive
                    }
                    false -> networkAvailabilityManager.triggerOfflineError { loadResults(isRefresh) }
                }
            } else {
                val result = (if (isRefresh) isDataRefreshing else isDataLoading)
                    .wrap { apsRepo.searchCustomerPickupOrders(siteId = user.selectedStoreId, onlyPickupReady = true) }

                when (networkAvailabilityManager.isConnected.first()) {
                    true -> {
                        when (result) {
                            is ApiResult.Success<List<ErDto>> -> {

                                val resultList = result.data.filter {
                                    it.status == ErOrderStatus.DROPPED_OFF && it.fulfillment?.subType != FulfillmentSubType.ONEPL
                                }.map {
                                    OrderItemUI(it)
                                }
                                results.postValue(resultList)
                            }
                            is ApiResult.Failure -> {
                                results.notifyObservers()
                                handleApiError(result, retryAction = { loadResults() })
                            }
                        }.exhaustive
                    }
                    false -> networkAvailabilityManager.triggerOfflineError { loadResults(isRefresh) }
                }
            }

            // Reset loadEvent so the StateFlow won't conflate multiple true requests together.
            loadEvent.value = null
        }
    }
    private fun checkForArrivedOrders() {
        viewModelScope.launch(dispatcherProvider.IO) {
            if (networkAvailabilityManager.isConnected.first()) {
                userRepo.user.value?.selectedStoreId?.let { siteId ->
                    val result = apsRepo.getCustomerArrivalsCount(siteId)
                    (result as? ApiResult.Success)?.data?.let {
                        badgeArrivalsAction.postValue(it.customerArrivalsCount.getOrZero() > 0)
                        return@launch
                    }
                    badgeArrivalsAction.postValue(false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activityViewModel.is1Pl.removeObserver(::switchNavigationIconAndText)
        badgeArrivalsAction.removeObserver(::switchNavigationIconAndText)
    }

    private fun get1PlDialog() = CustomDialogArgData(
        dialogType = DialogType.OnePlUnwantedItemRemovalConfirmation,
        title = R.string.one_pl_arrivals.toIdHelper(),
        cancelOnTouchOutside = false
    )

    companion object {
        private const val ONE_PL_DIALOG = "one_pl_dialog"
    }
}
