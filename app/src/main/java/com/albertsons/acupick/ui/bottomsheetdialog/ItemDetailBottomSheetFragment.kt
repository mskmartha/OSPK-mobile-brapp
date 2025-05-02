package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemDetailBottomSheetBinding
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.itemdetails.ItemDetailsParams
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel.Companion.COMPLETE_PICK_REQUEST_KEY
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel.Companion.COMPLETE_PICK_RESULTS
import com.albertsons.acupick.ui.picklistitems.PickListType
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/** Item details (for a specific item in a Pick List) */
class ItemDetailBottomSheetFragment : BaseBottomSheetDialogFragment() {

    // TODO: ACURED_REDESIGN need to be refactor once UI is freezed
    private val itemDetailViewModel: ItemDetailsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ItemDetailBottomSheetBinding>(inflater, R.layout.item_detail_bottom_sheet, container, false).apply {
            val args = argData.customDataParcel as ItemDetailsParams
            viewData = argData.toViewData(requireContext())
            itemDetailViewModel.apply {
                isFromSubstitutionFlow.value = args.isFromSubstitutionFlow
                iaId.value = args.iaId
                actId = args.actId
                pickNumber.value = args.activityNo
                altLocations.value = args.altItemLocations
                pickListType = args.pickListType

                navigation.observe(viewLifecycleOwner) { closeAction ->
                    if (args.pickListType == PickListType.Todo) {
                        dismissBottomSheet()
                    }

                    Timber.v("[ItemDetailBottomSheetFragment setupBinding closeActionEvent] closeAction=$closeAction")
                    // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                    findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
                }
                completePickCTAClicked.observe(viewLifecycleOwner) {
                    requireParentFragment().setFragmentResult(
                        COMPLETE_PICK_REQUEST_KEY,
                        bundleOf(COMPLETE_PICK_RESULTS to it)
                    )
                    dismissBottomSheet()
                }
            }
            viewModel = itemDetailViewModel

            if (args.isMoveToLocation) {
                scrollDown(scrollView)
            }

            sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                if (it.exit) {
                    dismissBottomSheet()
                }
            }

            itemDetailViewModel.pluCtaEvent.observe(viewLifecycleOwner) {
                parentFragment?.setFragmentResult(
                    ItemDetailsViewModel.ITEM_DETAILS_PLU_REQUEST_KEY,
                    bundleOf()
                )
            }

            itemDetailViewModel.addLocationEvent.observe(viewLifecycleOwner) {
                parentFragment?.setFragmentResult(
                    ItemDetailsViewModel.ADD_LOCATION_REQUEST_KEY,
                    bundleOf()
                )
            }

            itemDetailViewModel.locationCtaEvent.observe(viewLifecycleOwner) {
                scrollDown(scrollView)
            }
        }
    }

    private fun scrollDown(scrollView: NestedScrollView) {
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }
}
