package com.albertsons.acupick.ui.arrivals.complete

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.databinding.HandOffBottomSheetBinding
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.util.EventAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class HandOffBottomSheetDialogFragment(
    private val isCompleteButtonEnabled: Boolean
) : BottomSheetDialogFragment() {

    private val fragmentViewModel: HandOffBottomSheetDialogViewModel by viewModel()
    private var behavior: BottomSheetBehavior<FrameLayout>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = HandOffBottomSheetBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentViewModel.apply {
                isCompleteEnabled.postValue(isCompleteButtonEnabled)
                completeClickAction.observe(viewLifecycleOwner) { closeAction ->
                    dismiss()
                    // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                    findDialogListener()?.onCloseAction(closeAction, null)
                }
                headerClickAction.observe(viewLifecycleOwner) {
                    behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, BOTTOM_SHEET_NAME))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_OPEN, listPair)

        dialog?.setOnShowListener { dialogInterface ->

            val bottomSheetDialog = dialogInterface as? BottomSheetDialog
            val bottomSheet = bottomSheetDialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                behavior = BottomSheetBehavior.from(it)
                behavior?.peekHeight = HEADER_HEIGHT

                // Initially show just the header without animation (sliding up)
                dialog?.window?.setWindowAnimations(DISABLE_WINDOW_ANIMATIONS)
                behavior?.state = BottomSheetBehavior.STATE_COLLAPSED

                // Then immediately slide up the bottom sheet with animation
                dialog?.window?.setWindowAnimations(ENABLE_WINDOW_ANIMATIONS)
                behavior?.state = BottomSheetBehavior.STATE_EXPANDED

                behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {

                        // After collapsing the bottom sheet, remove the header without animation
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            dialog?.window?.setWindowAnimations(DISABLE_WINDOW_ANIMATIONS)
                            dismiss()
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Update isExpanded when bottom sheet is at the midway point -- used to point the chevron at the correct direction
                        fragmentViewModel.isExpanded.postValue(slideOffset > 0.5)
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, BOTTOM_SHEET_NAME))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_CLOSE, listPair)
    }

    companion object {
        fun newInstance(isCompleteButtonEnabled: Boolean) = HandOffBottomSheetDialogFragment(isCompleteButtonEnabled)
        const val HEADER_HEIGHT = 100 // pixels
        const val DISABLE_WINDOW_ANIMATIONS = -1
        const val ENABLE_WINDOW_ANIMATIONS = 0
        const val BOTTOM_SHEET_NAME = "HandOffBottomSheet"
    }
}
