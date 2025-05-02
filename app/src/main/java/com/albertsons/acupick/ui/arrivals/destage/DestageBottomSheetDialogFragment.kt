package com.albertsons.acupick.ui.arrivals.destage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.databinding.DestageBottomSheetFragmentBinding
import com.albertsons.acupick.ui.bindingadapters.setVisibilityGoneIfTrue
import com.albertsons.acupick.ui.util.EventAction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DestageBottomSheetDialogFragment(private val isPartialRx: Boolean, listener: DestageDialogListener) : BottomSheetDialogFragment() {

    private val fragmentViewModel: DestageBottomSheetViewModel by viewModel()
    private var destageDialogListener: DestageDialogListener? = null

    init {
        this.destageDialogListener = listener
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DestageBottomSheetFragmentBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentViewModel.apply {
                reportIssueClickAction.observe(viewLifecycleOwner) {
                    destageDialogListener?.reportIssue()
                    dismiss()
                }
                abandonPartialPrescriptionPickupClickAction.observe(viewLifecycleOwner) {
                    destageDialogListener?.abandonPartialPrescriptionPickup()
                    dismiss()
                }
                cancelClickAction.observe(viewLifecycleOwner) {
                    dismiss()
                }
            }
        }
        binding.clAbandonPrescriptionPickup.setVisibilityGoneIfTrue(!isPartialRx)
        binding.clReportIssue.setVisibilityGoneIfTrue(isPartialRx)
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

    fun setupDestageDialogListener(listener: DestageDialogListener) {
        this.destageDialogListener = listener
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            destageDialogListener = context as DestageDialogListener?
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    interface DestageDialogListener {
        fun reportIssue()
        fun abandonPartialPrescriptionPickup()
    }
    companion object {
        const val BOTTOM_SHEET_NAME = "DestageBottomSheet"
        fun newInstance(isPartialRx: Boolean, listener: DestageDialogListener) = DestageBottomSheetDialogFragment(isPartialRx, listener).apply {
            setupDestageDialogListener(listener)
        }
    }
}
