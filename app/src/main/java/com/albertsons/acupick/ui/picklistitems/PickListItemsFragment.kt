package com.albertsons.acupick.ui.picklistitems

import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.FulfilledQuantityResult
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.databinding.PickListItemsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.bottomsheetdialog.ConfirmAmountBottomSheet.Companion.CONFIRM_AMOUNT_REQUEST
import com.albertsons.acupick.ui.bottomsheetdialog.ConfirmAmountBottomSheet.Companion.CONFIRM_AMOUNT_REQUEST_RESULT
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import com.albertsons.acupick.ui.dialog.BaseCustomDialogFragment
import com.albertsons.acupick.ui.dialog.CustomDialogArgData
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.itemdetails.UnPickResultParams
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.BARCODE_TYPE
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.BYPASS_QUANTITY_PICKER
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK_RESULTS
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPickResults
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationResultParams
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationViewModel
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.models.PickListScannedData
import com.albertsons.acupick.ui.notification.NotificationViewModel
import com.albertsons.acupick.ui.picklistitems.PickListItemsViewModel.Companion.SYNC_FAILED_DIALOG_TAG
import com.albertsons.acupick.ui.substitute.SubstituteViewModel
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.KEY_ITEM_HAS_UPDATED
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.NAVIGATE_BACK_FROM_SUBSTITUTION_UI
import com.albertsons.acupick.ui.substitute.SubstitutionLocalItem
import com.albertsons.acupick.ui.util.AcupickSnackbar
import com.albertsons.acupick.ui.util.OnSnapPositionChangeListener
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.SnapOnScrollListener
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import com.albertsons.acupick.ui.util.attachSnapHelperWithListener
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/** Single Pick List showing all items (primary picker working screen) */
class PickListItemsFragment : BaseFragment<PickListItemsViewModel, PickListItemsFragmentBinding>() {
    override val fragmentViewModel: PickListItemsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    private val notificationViewModel: NotificationViewModel by sharedViewModel()

    override fun getLayoutRes(): Int = R.layout.pick_list_items_fragment

    private val args: PickListItemsFragmentArgs by navArgs()

    private val userFeedback by inject<UserFeedback>()

    val snapHelpers = mutableMapOf<String, LinearSnapHelper>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentViewModel.loadPickList(args.activityId, args.endPick, args.toteEstimate)
        if (args.navigateToChat) {
            // @Apath11 todo pass parameters related required for chat
            fragmentViewModel.onChatClicked(args.orderNumber)
        }
    }

    override fun setupBinding(binding: PickListItemsFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(fragmentViewModel.currentTab.value?.value ?: 0))
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                fragmentViewModel.updatePickingStatus()
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                fragmentViewModel.setCurrentTab(tab?.position)
                fragmentViewModel.scannedItemFailure.postValue(false)
            }
        })
        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            }, activityViewModel)
        }

        fragmentViewModel.isListView.observe(this) {
            setupRecyclerViewSnapOnScrollListener(binding.todoItemsRecyclerView, it, "todo")
            setupRecyclerViewSnapOnScrollListener(binding.pickedItemsRecyclerView, it, "picked")
            setupRecyclerViewSnapOnScrollListener(binding.shortItemsRecyclerView, it, "short")
        }

        fragmentViewModel.pickRepository.syncError.observe(viewLifecycleOwner) {
            val argData = CustomDialogArgData(
                titleIcon = R.drawable.ic_alert,
                title = StringIdHelper.Id(R.string.sync_failed),
                body = StringIdHelper.Id(R.string.sync_failed_body),
                positiveButtonText = StringIdHelper.Id(R.string.ok),
                cancelOnTouchOutside = false
            )
            BaseCustomDialogFragment.newInstance(argData).showWithFragment(this, SYNC_FAILED_DIALOG_TAG)
        }
        activityViewModel.scannedData.observe(viewLifecycleOwner) {
            fragmentViewModel.onScannerBarcodeReceived(it)
        }
        fragmentViewModel.scrollToPosition.observe(viewLifecycleOwner) { pos ->
            when (binding.tabLayout.selectedTabPosition) {
                0 -> {
                    binding.todoItemsRecyclerView.adapter?.notifyDataSetChanged()
                    binding.todoItemsRecyclerView.smoothScrollToPosition(pos)
                }
                1 -> {
                    binding.pickedItemsRecyclerView.adapter?.notifyDataSetChanged()
                    binding.pickedItemsRecyclerView.smoothScrollToPosition(pos)
                }
                2 -> {
                    binding.shortItemsRecyclerView.adapter?.notifyDataSetChanged()
                    binding.shortItemsRecyclerView.smoothScrollToPosition(pos)
                }
            }
        }
        fragmentViewModel.playScanSound.observe(viewLifecycleOwner) { isSuccess ->
            when (isSuccess) {
                true -> userFeedback.setSuccessScannedSoundAndHaptic()
                false -> userFeedback.setFailureScannedSoundAndHaptic()
            }
        }
        fragmentViewModel.unAssignSuccessfulAction.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
        fragmentViewModel.assignedToWrongUserAction.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
        fragmentViewModel.todoItemCount.observe(viewLifecycleOwner) {
            binding.tabLayout.getTabAt(0)?.text = getString(R.string.pick_list_items_tab_todo_format, it)
        }
        fragmentViewModel.pickedItemCount.observe(viewLifecycleOwner) {
            binding.tabLayout.getTabAt(1)?.text = getString(R.string.pick_list_items_tab_picked_format, it)
        }
        fragmentViewModel.shortItemCount.observe(viewLifecycleOwner) {
            binding.tabLayout.getTabAt(2)?.text = getString(R.string.pick_list_items_tab_short_format, it)
        }
        fragmentViewModel.currentTab.observe(viewLifecycleOwner) {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(it.value))
        }

        fragmentViewModel.isBlockingUi.observe(viewLifecycleOwner) {
            activityViewModel.setLoadingState(it, true)
        }
        notificationViewModel.endPickAction.observe(viewLifecycleOwner) {
            fragmentViewModel.onEndPickCtaClicked(true)
        }
        fragmentViewModel.canAcceptScan = true
        setUpFragmentResultListeners()

        // TODO: ACURED_REDESIGN Will refactor by using utility class
        // Show snackbar message if item has substituted/OOS/PrepNotReady/ToteFull once get back to this fragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<StringIdHelper>(KEY_ITEM_HAS_UPDATED)?.observe(viewLifecycleOwner) { message ->
            fragmentViewModel.showSnackBar(AcupickSnackEvent(message = message, SnackType.SUCCESS))
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<StringIdHelper>(KEY_ITEM_HAS_UPDATED)
        }

        // Show selected item detail bottomsheet once get back from substitute UI to the picklist UI in listview mode
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(NAVIGATE_BACK_FROM_SUBSTITUTION_UI)?.observe(viewLifecycleOwner) {
            fragmentViewModel.openLastSelectedItemDetailBottomSheet()
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>(NAVIGATE_BACK_FROM_SUBSTITUTION_UI)
        }

        fragmentViewModel.acupickSnackEventAnchored.observe(viewLifecycleOwner) {
            AcupickSnackbar.make(this@PickListItemsFragment, it)
                .setAnchorView(binding.scanItemTv)
                .show()
        }
    }

    private fun setUpFragmentResultListeners() {
        requireActivity().supportFragmentManager.setFragmentResultListener(MANUAL_ENTRY_PICK, this) { _, bundle ->
            Timber.d("1292 pickListItemsListener manualEntryPick listener setFragmentResult")
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(MANUAL_ENTRY_PICK_RESULTS)
            fragmentViewModel.canAcceptScan = true
            (manualEntryResults as? ManualEntryPickResults)?.let { result ->
                fragmentViewModel.lastScannedItemDetails = result.itemDetails
                fragmentViewModel.lastItemBarcodeScanned = result.barcode as? BarcodeType.Item
                fragmentViewModel.isFromManualEntry = true
                fragmentViewModel.handleManualEntryResults(result.quantity, result.barcode as? BarcodeType.Item)
            }
        }

        setFragmentResultListener(BYPASS_QUANTITY_PICKER) { _, bundle ->
            val serializableResult = bundle.get(BARCODE_TYPE)
            (serializableResult as BarcodeType).let {
                fragmentViewModel.handleManualEntryScan(it)
            }
        }

        setFragmentResultListener(CONFIRM_AMOUNT_REQUEST) { _, bundle ->
            val serializableResult = bundle.getSerializable(CONFIRM_AMOUNT_REQUEST_RESULT)
            (serializableResult as? FulfilledQuantityResult.ConfirmNetWeightResult)?.let { confirmNetWeightResult ->
                Timber.d("Confirm Net Weight Result: $confirmNetWeightResult")
                fragmentViewModel.handleFulfilledQuantityResult(confirmNetWeightResult)
            }
        }

        setFragmentResultListener(ItemDetailsViewModel.ITEM_DETAILS_PLU_REQUEST_KEY) { _, _ ->
            fragmentViewModel.handlePluCtaResult()
        }
        setFragmentResultListener(ItemDetailsViewModel.ADD_LOCATION_REQUEST_KEY) { _, _ ->
            fragmentViewModel.onAddLocationClicked()
        }
        setFragmentResultListener(MissingItemLocationViewModel.WHERE_TO_FIND_LOCATION_CODE_REQUEST_KEY) { _, _ ->
            fragmentViewModel.onWhereToFindLocationCode()
        }
        setFragmentResultListener(MissingItemLocationViewModel.MISSING_ITEM_LOCATION_REQUEST_KEY) { _, bundle ->
            val missingItemLocationResults = bundle.get(MissingItemLocationViewModel.MISSING_ITEM_LOCATION_RESULTS)
            val scannedData = bundle.get(MissingItemLocationViewModel.DATA_PICKSLIST_SCANNED) as? PickListScannedData?
            (missingItemLocationResults as? MissingItemLocationResultParams)?.let {
                fragmentViewModel.onMissingItemLocationAdded(missingItemLocationResults, scannedData)
            }
        }

        setFragmentResultListener(MissingItemLocationViewModel.MISSING_ITEM_LOCATION_NOT_NOW_REQUEST_KEY) { _, bundle ->
            val scannedData = bundle.get(MissingItemLocationViewModel.DATA_PICKSLIST_SCANNED) as? PickListScannedData?
            viewLifecycleOwner.lifecycleScope.launch {
                scannedData?.let {
                    // Handled manual entry flow
                    when (fragmentViewModel.isFromManualEntry) {
                        true -> fragmentViewModel.openToteScanBottomSheet()
                        else -> it.scannedBarcodeResult?.let { barcodeResult ->
                            fragmentViewModel.handleMatchedItem(it.item, barcodeResult)
                        }
                    }
                }
            }
        }

        setFragmentResultListener(ItemDetailsViewModel.COMPLETE_PICK_REQUEST_KEY) { _, bundle ->
            val itemPicked = bundle.get(ItemDetailsViewModel.COMPLETE_PICK_RESULTS)
            fragmentViewModel.onCompletePickClicked(itemPicked as ItemActivityDto)
        }
        setFragmentResultListener(ItemDetailsViewModel.UNPICK_RESULT_KEY) { _, bundle ->
            Timber.d("Unpick action listener")
            viewLifecycleOwner.lifecycleScope.launch {
                fragmentViewModel.prepareForUndoPick((bundle.get(ItemDetailsViewModel.UNPICK_RESULT_DATA_KEY) as? UnPickResultParams)?.checkedItems)
            }
        }

        // ISSUE-SCANNING recieve item remove event of issue scanning
        setFragmentResultListener(SubstituteViewModel.REMOVE_SUBSTITUTION_REQUEST_KEY) { _, bundle ->
            val itemRemove = bundle.get(SubstituteViewModel.REMOVE_SUBSTITUTION_RESULTS)
            fragmentViewModel.showDeleteIssueScannedItemDialog(itemRemove as SubstitutionLocalItem)
        }

        activityViewModel.bottomSheetBackButtonPressed.observe(viewLifecycleOwner) {
            fragmentViewModel.showExitIssueScanningConfirmationBottomSheet()
        }
    }

    override fun onPause() {
        super.onPause()
        fragmentViewModel.clearInvalidItemScanTracker()
    }

    /**
     * @param recyclerView Attach or Remove from SnapHelper
     * @param isListView Removes the recyclerview from the SnapHelper if true, otherwise attaches it
     * @param type To get or create the SnapHelper for particular recyclerview
     */
    private fun setupRecyclerViewSnapOnScrollListener(recyclerView: RecyclerView, isListView: Boolean, type: String) {
        if (isListView) {
            getNewSnapHelper(isListView, type)?.attachToRecyclerView(null)
        } else {
            recyclerView.attachSnapHelperWithListener(
                getNewSnapHelper(isListView, type)!!,
                SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
                object : OnSnapPositionChangeListener {
                    override fun onSnapPositionChange(position: Int) {
                        Timber.v("[onSnapPositionChange] position=$position")
                        fragmentViewModel.updateSelectedItemIndex(position)
                        fragmentViewModel.showPersistentSnackbarPrompt(showOrderIssue = false)
                    }
                }
            )
        }
    }

    /**
     * @param isListView If true, removes the SnapHelper from map. Otherwise get or put it to map
     * @param type To create or remove SnapHelper for each key/recyclerview type
     * @return returns the SnapHelper, for ListView may return null if not present in map
     */
    private fun getNewSnapHelper(isListView: Boolean, type: String): LinearSnapHelper? {
        if (isListView) return snapHelpers.remove(type)
        return snapHelpers.getOrPut(type) { LinearSnapHelper() }
    }
}
