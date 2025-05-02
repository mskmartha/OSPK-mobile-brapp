package com.albertsons.acupick.ui.arrivals.destage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.databinding.ArrivalsOptionsBottomSheetBinding
import com.albertsons.acupick.ui.util.EventAction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArrivalsOptionsBottomSheetDialogFragment(listener: () -> Unit = {}) : BottomSheetDialogFragment() {

    private val fragmentViewModel: ArrivalsOptionsViewModel by viewModel()
    private var markAsNotHereListener: () -> Unit = {}

    init {
        markAsNotHereListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ArrivalsOptionsBottomSheetBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentViewModel.apply {
                cancelClickAction.observe(viewLifecycleOwner) {
                    dismiss()
                }
                markAsNotHereAction.observe(viewLifecycleOwner) {
                    markAsNotHereListener()
                    dismiss()
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, BOTTOM_SHEET_NAME))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_OPEN, listPair)
    }

    override fun onDestroy() {
        super.onDestroy()
        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, BOTTOM_SHEET_NAME))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_CLOSE, listPair)
    }

    companion object {
        const val MARK_AS_NOT_HERE_SHEET_TAG = "ArrivalsOptionsBottomSheetDialogFragmentTag"
        const val BOTTOM_SHEET_NAME = "ArrivalsOptionsBottomSheet"

        fun newInsance(listener: () -> Unit = {}) = ArrivalsOptionsBottomSheetDialogFragment(listener)
    }
}
