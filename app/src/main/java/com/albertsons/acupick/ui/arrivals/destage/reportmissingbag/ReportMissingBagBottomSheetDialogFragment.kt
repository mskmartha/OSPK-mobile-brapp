package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.ui.util.EventAction
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.ui.util.orTrue
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ReportMissingBagBottomSheetDialogFragment(listener: DestageBagBypassDialogListener, val isMfcSite: Boolean, val isCurrentOrderHasLooseItem: Boolean, val isCustomerBagPreference: Boolean) :
    BottomSheetDialogFragment() {

    private val fragmentViewModel: ReportMissingBagSheetViewModel by viewModel()
    private var destageDialogListener: DestageBagBypassDialogListener? = null

    init {
        this.destageDialogListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = BaseComposeViewBottomSheetBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            baseComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    ReportMissingBagOrToteBottomSheetScreen(fragmentViewModel, isMfcSite, isCurrentOrderHasLooseItem, isCustomerBagPreference.orTrue())
                }
            }
            fragmentViewModel.apply {
                collectFlow(cancelClickAction) {
                    destageDialogListener?.cancelClicked()
                    dismiss()
                }
                collectFlow(missingBagLabelAction) {
                    destageDialogListener?.missingBagLabelClicked()
                    dismiss()
                }
                collectFlow(missingBagAction) {
                    destageDialogListener?.missingBagClicked()
                    dismiss()
                }
                collectFlow(missingLooseItemLabelAction) {
                    destageDialogListener?.missingLooseItemLabelClicked()
                    dismiss()
                }
                collectFlow(missingLooseItemAction) {
                    destageDialogListener?.missingLooseItemClicked()
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

    fun setupDestageDialogListener(listener: DestageBagBypassDialogListener) {
        this.destageDialogListener = listener
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            destageDialogListener = context as DestageBagBypassDialogListener?
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    interface DestageBagBypassDialogListener {
        fun missingLooseItemClicked()
        fun missingLooseItemLabelClicked()
        fun missingBagLabelClicked()
        fun missingBagClicked()
        fun cancelClicked()
    }
    companion object {
        const val REPORT_MISSING_BAG_SHEET_TAG = "ReportMissingBagBottomSheetDialogFragmentTag"
        const val BOTTOM_SHEET_NAME = "ReportMissingBagBottomSheet"
        fun newInstance(listener: DestageBagBypassDialogListener, isMfcSite: Boolean, isCurrentOrderHasLooseItem: Boolean, isCustomerBagPreference: Boolean) =
            ReportMissingBagBottomSheetDialogFragment(
                listener, isMfcSite, isCurrentOrderHasLooseItem,
                isCustomerBagPreference
            ).apply {
                setupDestageDialogListener(listener)
            }
    }
}
