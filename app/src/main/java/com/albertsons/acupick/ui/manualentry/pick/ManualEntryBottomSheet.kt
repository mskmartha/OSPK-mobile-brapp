package com.albertsons.acupick.ui.manualentry.pick

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ManualEntryBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

class ManualEntryBottomSheet : BottomSheetDialogFragment() {

    private val args by navArgs<ManualEntryBottomSheetArgs>()
    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DataBindingUtil.inflate<ManualEntryBottomsheetBinding>(inflater, R.layout.manual_entry_bottomsheet, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        val manualEntryPagerFragment = ManualEntryPagerFragment().apply {
            arguments = bundleOf("manualEntryParams" to args.manualEntryParams, "entryType" to args.entryType)
        }
        // checking if the fragment is already added to the fragment manager
        if (!childFragmentManager.isStateSaved) {
            val transaction = childFragmentManager.beginTransaction()
            transaction.replace(R.id.manualEntryFragmentContainer, manualEntryPagerFragment).commit()
        }

        return binding.root
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
                if (args.manualEntryParams.isSubstitution || args.manualEntryParams.isIssueScanning) {
                    standardBottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.expanded_bottomsheet_peek_height)
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        val bottomSheetDialog = dialog as? BottomSheetDialog
        // checking if the bottom sheet is showing and not in hidden state
        if (bottomSheetDialog?.isShowing == true && standardBottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            super.onCancel(dialog)
        }
        Timber.v("[onCancel] sending CloseAction.Dismiss for ManualEntry Fragment")
        requireActivity().supportFragmentManager.setFragmentResult(
            ManualEntryPagerViewModel.MANUAL_ENTRY_PICK,
            Bundle()
        )
    }
}
