package com.albertsons.acupick.ui.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BulkSubstituteConfirmationBottomSheetBinding
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.substitute.BulkSubstituteConfirmationParam
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/** Bottom sheet for bulk substitute item */
class BulkSubstituteBottomSheet : BaseBottomSheetDialogFragment() {
    private val bulkSubstituteConfirmationViewModel: BulkSubstituteConfirmationViewModel by viewModel()
    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<View>
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BulkSubstituteConfirmationBottomSheetBinding>(inflater, R.layout.bulk_substitute_confirmation_bottom_sheet, container, false).apply {
            viewData = argData.toViewData(requireContext())
            val substituteData = argData.customDataParcel as BulkSubstituteConfirmationParam
            lifecycleOwner = viewLifecycleOwner
            viewModel = bulkSubstituteConfirmationViewModel
            sharedViewModel.isLoading.observe(viewLifecycleOwner) {
                isLoading = it
            }

            // invalidate the screen because setting the data did not cause redraw
            bulkSubstituteConfirmationViewModel.bulkSubList.observe(viewLifecycleOwner) {
                invalidateAll()
            }

            bulkSubstituteConfirmationViewModel.setData(substituteData.bulkItems)
            bulkSubstituteConfirmationViewModel.navigation.observe(viewLifecycleOwner) { closeAction ->
                Timber.v("[Substitute confirmation bottomsheet setupBinding closeActionEvent] closeAction=$closeAction")
                dismiss() // Dismiss on confirm button clicked
                findDialogListener()?.onCloseAction(closeAction.first, closeAction.second?.toInt())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog
            bottomSheetDialog?.dismissWithAnimation = true
            val bottomSheet = bottomSheetDialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                standardBottomSheetBehavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                standardBottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(argData.peekHeight)
                standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}
