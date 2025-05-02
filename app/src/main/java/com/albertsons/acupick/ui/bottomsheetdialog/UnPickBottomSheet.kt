package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.UnPickBinding
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.itemdetails.UnPickParams
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class UnPickBottomSheet : BaseBottomSheetDialogFragment() {

    // TODO: ACURED_REDESIGN need to be refactor once UI is freezed
    private val itemDetailViewModel: ItemDetailsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<UnPickBinding>(inflater, R.layout.un_pick, container, false).apply {
            val args = argData.customDataParcel as UnPickParams
            viewData = argData.toViewData(requireContext())
            fragmentViewLifecycleOwner = viewLifecycleOwner
            itemDetailViewModel.apply {
                iaId.value = args.iaId
                actId = args.actId
                pickNumber.value = args.activityNo
                pickListType = args.pickListType
                navigation.observe(viewLifecycleOwner) { closeAction ->
                    Timber.v("[ItemDetailBottomSheetFragment setupBinding closeActionEvent] closeAction=$closeAction")
                    // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                    findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
                }
            }
            viewModel = itemDetailViewModel
            sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                if (it.exit && it.dialogType == BottomSheetType.UnPick) {
                    requireParentFragment().setFragmentResult(
                        ItemDetailsViewModel.UNPICK_RESULT_KEY,
                        bundleOf(ItemDetailsViewModel.UNPICK_RESULT_DATA_KEY to it.customDataParcel)
                    )
                    dismiss()
                }
            }
        }
    }
}
