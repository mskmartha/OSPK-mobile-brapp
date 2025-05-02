package com.albertsons.acupick.ui.arrivals.destage

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.albertsons.acupick.R
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.databinding.DestageOrderPagerFragmentBinding
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejectedItemFragment.Companion.CANCELED_REMOVE_ITEMS
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejectedItemFragment.Companion.CANCELED_REMOVE_ITEMS_RESULT
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejectedItemFragment.Companion.REMOVED_ITEMS
import com.albertsons.acupick.ui.arrivals.destage.removeitems.RemoveRejectedItemFragment.Companion.REMOVED_ITEMS_RESULT
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.AddToHandoffUI
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.MarkedArrivedUI
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add.AddCustomerViewModel.Companion.ADD_CUSTOMER_RETURN
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.add.AddCustomerViewModel.Companion.ADD_CUSTOMER_RETURN_RESULT
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus.ChangeCustomerStatusViewModel.Companion.UPDATE_CUSTOMER_STATUS_RETURN
import com.albertsons.acupick.ui.arrivals.destage.updatecustomers.changestatus.ChangeCustomerStatusViewModel.Companion.UPDATE_CUSTOMER_STATUS_RETURN_RESULT
import com.albertsons.acupick.ui.arrivals.pharmacy.ManualEntryPharmacyData
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryBaseViewModel.Companion.MANUAL_ENTRY_HANDOFF_RESULTS
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryHandOffBag
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyViewModel
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.staging.OFFSCREEN_PAGE_LIMIT
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.orTrue
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class DestageOrderPagerFragment : BaseFragment<DestageOrderPagerViewModel, DestageOrderPagerFragmentBinding>() {

    // via Nav Graph
    override val fragmentViewModel: DestageOrderPagerViewModel by navGraphViewModels(R.id.destageOrderScope)
    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun getLayoutRes(): Int = R.layout.destage_order_pager_fragment
    private val args: DestageOrderPagerFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            fragmentViewModel.handleExitButton()
        }
    }

    private var mediator: TabLayoutMediator? = null
    lateinit var callback: ViewPager2.OnPageChangeCallback

    override fun setupBinding(binding: DestageOrderPagerFragmentBinding) {
        super.setupBinding(binding)

        fragmentViewModel.setupCustomerData(args.customerData)
        fragmentViewModel.setupOrderDetails(args.activityList?.activityList ?: listOf())
        fragmentViewModel.isFromNotification.value = args.isFromNotification

        with(activityViewModel) {

            // Observe main scan event and relay to view model
            scannedData.observe(viewLifecycleOwner) {
                fragmentViewModel.onScannerBarcodeReceived(it)
            }
        }

        notificationViewModel.addHandoffFromNotification.observe(viewLifecycleOwner) {
            // To dismiss manual entry bottomsheet if open in case of incoming batch handoff through DUG interjection
            if (fragmentViewModel.isManualEntryBottomSheetOpen) {
                activityViewModel.bottomSheetRecordPickArgData.postValue(fragmentViewModel.getManualEntryBottomSheetDismissArgData())
            }
            fragmentViewModel.assignToMe(it)
        }

        // DUG interjection to show batch failure message
        notificationViewModel.notificationMessageSnackEvent.observe(viewLifecycleOwner) { snackBarEvent ->
            snackBarEvent.let { fragmentViewModel.showSnackBar(it) }
        }

        with(fragmentViewModel) {
            launchStaffMemberRequiredModal.observe(viewLifecycleOwner) { launchModal ->
                if (launchModal) fragmentViewModel.showRxStaffRequiredDialog()
            }
            activityViewModel.isLoading.observe(viewLifecycleOwner) {
                isLoading.postValue(it)
            }

            isBlockingUi.observe(viewLifecycleOwner) {
                activityViewModel.setLoadingState(it, false)
            }

            markedArrivedSnackBarEvent.observe(viewLifecycleOwner) {
                // TODO: DUG Interjection Will remove this commented code after complete testing
                /*it?.let { message ->
                    val view = this@DestageOrderPagerFragment.requireView()
                    MarkedArrivedSnackBar.make(
                        view = view,
                        string = message,
                        showCta = false,
                        unit = {},
                        onDismissed = { fragmentViewModel.showIndefinitePrompt() }
                    ).show()
                }*/
            }

            orderIssuesButtonEvent.observe(viewLifecycleOwner) {
                previousPageEvent.value = pageEvent.value
                showOrderIssueScanBagDialog()
            }
            binding.destageOrderPagerViewPager.isSaveEnabled = true

            callback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    pageEvent.value = position
                    val orderUiData = resultsUiList.value?.get(position)
                    currentOrderNumber = orderUiData?.customerOrderNumber.orEmpty()
                    setCurrentActNo(orderUiData?.activityNo.orEmpty())
                    setActiveOderNumber(orderUiData?.customerOrderNumber)
                    validateCurrentOrderHasLooseItem()
                    isCurrentOrderMfc = orderUiData?.isMultiSource == true && orderUiData.type == ContainerType.TOTE
                    getActivityByOrder(currentOrderNumber).observe(viewLifecycleOwner) { destageOrderUiData ->
                        isCurrentOrderMultiSource = destageOrderUiData?.isMultiSource.orFalse()
                        isCurrentOrderHasCustomerBagPreference = destageOrderUiData?.detailsHeaderUi?.isCustomerBagPreference.orTrue()
                        destageOrderUiData?.let {
                            fragmentViewModel.destageOrderUiData = it
                        }
                    }
                    if (orderUiData?.customerOrderNumber != null) {
                        if (fragmentViewModel.noBagsMap[orderUiData.customerOrderNumber] == false && isCurrentOrderMfc) {
                            showRemoveBagsDialog(orderUiData.customerName.orEmpty())
                            fragmentViewModel.noBagsMap[orderUiData.customerOrderNumber] = true
                        }
                    }
                    checkComplete()
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    if (hideStaticPrompt.value == false) {
                        showIndefinitePrompt()
                    }
                }
            }

            binding.destageOrderPagerViewPager.registerOnPageChangeCallback(callback)
            viewLifecycleOwner.lifecycleScope.launch {
                delay(50)
                pageEvent.value = previousPageEvent.value
            }

            // Tabs are controlled by view model and relayed here to adapter
            tabs.observe(viewLifecycleOwner) { tabs ->
                with(binding.destageOrderPagerViewPager) {
                    adapter = DestageOrderPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, tabs)
                    (adapter as DestageOrderPagerAdapter).menuTabMediatorFactory(
                        tabs = binding.destageOrderPagerTabLayout,
                        pager = this
                    ).attach()
                }
                binding.destageOrderPagerTabLayout.tabMode = when (tabs.size) {
                    2 -> TabLayout.MODE_FIXED
                    else -> TabLayout.MODE_AUTO
                }

                // Point view model at starting position
                pageEvent.value = previousPageEvent.value

                notificationViewModel.userAlreadyAssignedToMaxOrders.postValue(tabs.size >= MAX_HANDOFF_COUNT)

                // DUG interjection validate Max handoff
                if (tabs.size >= MAX_INTERJECTION_HANDOFF_COUNT) {
                    setDugInterjectionState(DugInterjectionState.BatchFailureReason.MaxHandoffAssigned)
                }
            }

            lockTab.observe(viewLifecycleOwner) {

                mediator = TabLayoutMediator(binding.destageOrderPagerTabLayout, binding.destageOrderPagerViewPager) { tab, positon ->
                    tab.view.isClickable = !lockTab.value.orFalse()
                    tab.text = tabs.value?.get(positon)?.tabLabel
                }
                mediator?.attach()
            }

            // Show check mark on tabs which correspond to orders that are completed
            isCompleteList.observe(viewLifecycleOwner) {
                it.forEach { orderCompletionState ->
                    (binding.destageOrderPagerViewPager.adapter as DestageOrderPagerAdapter?)?.let { pagerAdapter ->
                        val index = pagerAdapter.getIndexOfOrder(orderCompletionState.customerOrderNumber)
                        val tab = binding.destageOrderPagerTabLayout.getTabAt(index)
                        tab?.icon = if (orderCompletionState.isComplete) {
                            val iconResId = if (tab?.isSelected == true) R.drawable.ic_check_dark_blue else R.drawable.ic_check_grey
                            ResourcesCompat.getDrawable(resources, iconResId, null)
                        } else {
                            null
                        }
                    }
                }
            }

            // Observe ViewModel page event and set view pager to appropriate position
            viewLifecycleOwner.lifecycleScope.launch {
                pageEvent.collect {
                    if (binding.destageOrderPagerViewPager.currentItem != it) binding.destageOrderPagerViewPager.currentItem = it
                }
            }
        }

        @SuppressLint("WrongConstant")
        binding.destageOrderPagerViewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT

        setUpFragmentResultListeners()
    }

    private fun setUpFragmentResultListeners() {
        setFragmentResultListener(MANUAL_ENTRY_HANDOFF) { _, bundle ->
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(MANUAL_ENTRY_HANDOFF_RESULTS)
            (manualEntryResults as? ManualEntryHandOffBag)?.let {
                Timber.d("ManualEntryHandoff listener triggered $it")
                fragmentViewModel.handleManualEntryBag(it)
            }
        }
        // To handle result from manual entry bottomsheet in destaging
        requireActivity().supportFragmentManager.setFragmentResultListener(MANUAL_ENTRY_HANDOFF, viewLifecycleOwner) { _, bundle ->
            val manualEntryResults = bundle.get(MANUAL_ENTRY_HANDOFF_RESULTS)
            (manualEntryResults as? ManualEntryHandOffBag)?.let {
                Timber.d("ManualEntryHandoff listener triggered $it")
                fragmentViewModel.isManualEntryBottomSheetOpen = false
                fragmentViewModel.handleManualEntryBag(it)
            }
        }

        setFragmentResultListener(REPORT_ISSUE_REQUEST) { requestKey: String, bundle: Bundle ->
            val reportIssueCancelled = bundle.getBoolean(REPORT_ISSUE_CANCELLED_KEY)
            val reportIssueSelection = bundle.getString(REPORT_ISSUE_SELECTION_KEY)
            val reportIssuePreviousPage = bundle.getInt(REPORT_ISSUE_PREVIOUS_PAGE)

            if (reportIssueCancelled) fragmentViewModel.cancelOrderIssue()
            if (reportIssueSelection.isNotNullOrEmpty()) fragmentViewModel.completeOrderIssue(reportIssueSelection)
            if (reportIssuePreviousPage > 0) {
                fragmentViewModel.changePage(reportIssuePreviousPage)
            }
        }
        setFragmentResultListener(ManualEntryPharmacyViewModel.MANUAL_ENTRY_PHARMACY) { _, bundle ->
            Timber.d("1292 MANUAL_ENTRY_WINE_STAGING Recieved")
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(ManualEntryPharmacyViewModel.MANUAL_ENTRY_PHARMACY_RESULTS)
            (manualEntryResults as? ManualEntryPharmacyData)?.let { stagingData ->
                stagingData.stagingContainer?.let {
                    fragmentViewModel.handleManualEntryPharmacy(stagingData)
                }
            }
        }

        setFragmentResultListener(ADD_CUSTOMER_RETURN) { _, bundle ->
            Timber.d("1292 DestageOrderPageListener ADD_CUSTOMER_RETURN listener setFragmentResult")
            val view = this@DestageOrderPagerFragment.requireView()
            // Any type can be passed via to the bundle
            val orderToAdd = bundle.get(ADD_CUSTOMER_RETURN_RESULT)
            (orderToAdd as? AddToHandoffUI)?.let {
                fragmentViewModel.loadDetails(erIds = orderToAdd.erIdIdList, newOrder = true)
                fragmentViewModel.onCustomerAdded(orderToAdd.snackBarData?.first)
            }
        }

        setFragmentResultListener(UPDATE_CUSTOMER_STATUS_RETURN) { _, bundle ->
            Timber.d("1292 DestageOrderPagerFragment listener setFragmentResult")
            // Any type can be passed via to the bundle
            val orderToUpdate = bundle.get(UPDATE_CUSTOMER_STATUS_RETURN_RESULT)
            val view = this@DestageOrderPagerFragment.requireView()
            val count = fragmentViewModel.tabs.value?.size ?: 1
            (orderToUpdate as? MarkedArrivedUI)?.let {
                fragmentViewModel.onCustomerUpdateStatus(count, orderToUpdate)
            }
        }

        setFragmentResultListener(REMOVED_ITEMS) { _, bundle ->
            val requests = bundle.get(REMOVED_ITEMS_RESULT)
            (requests as? List<RemoveItemsRequestDto>)?.let {
                with(fragmentViewModel) {
                    if (it.isNotEmpty()) {
                        this.acceptRejectedItems(it)
                    } else {
                        this.showRejectedItemSnackbar()
                    }
                }
            }
        }

        setFragmentResultListener(CANCELED_REMOVE_ITEMS) { _, bundle ->
            val requests = bundle.get(CANCELED_REMOVE_ITEMS_RESULT)
            (requests as? Boolean)?.let { canceledFromRejectItems ->
                fragmentViewModel.itemRemovalRequired.value = canceledFromRejectItems
            }
        }
        setFragmentResultListener(MANUAL_ENTRY_TOOL_TIP_TAG_REQUEST_KEY) { _, bundle ->
            fragmentViewModel.getManualEntryToolTipBottomsheetDialog()
        }
    }

    override fun onDestroyView(binding: DestageOrderPagerFragmentBinding) {
        binding.apply { destageOrderPagerViewPager.adapter = null }
        super.onDestroyView(binding)
    }

    companion object {
        const val REPORT_ISSUE_REQUEST = "700"
        const val REPORT_ISSUE_CANCELLED_KEY = "800"
        const val REPORT_ISSUE_SELECTION_KEY = "900"
        const val REPORT_ISSUE_PREVIOUS_PAGE = "1010"
        const val MANUAL_ENTRY_TOOL_TIP_TAG_REQUEST_KEY = "missing_manual_entry_tool_tip_key"
    }
}
