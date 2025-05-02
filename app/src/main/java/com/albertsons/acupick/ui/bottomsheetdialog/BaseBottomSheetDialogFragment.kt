package com.albertsons.acupick.ui.bottomsheetdialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.EventCategory
import com.albertsons.acupick.EventKey
import com.albertsons.acupick.EventLabel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffRemoveItemsBottomSheet
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.manualentry.handoff.ManualEntryToolTipBottomSheet
import com.albertsons.acupick.ui.manualentry.pharmacy.ManualEntryPharmacyBottomSheet
import com.albertsons.acupick.ui.util.AcupickSnackbar
import com.albertsons.acupick.ui.util.EventAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Abstract custom bottom sheet class that displays data (via [ArgData]) styled for the app. Handles bottom sheet [CloseAction] via [CloseActionListenerProvider] on host
 * fragment/activity/[CloseActionListener]
 * for each bottom sheet displayed by the fragment/activity.
 *
 * ### How to use:
 * 1.  Extend this class, overriding [getViewDataBinding].
 * 2.  Make the host fragment/activity implement [CloseActionListenerProvider].
 * 3.  Call [BaseBottomSheetDialogFragment.newInstance] with data to be shown in the bottom sheet.
 * 4a. Call [showWithFragment] on the [CustomBottomSheetFragment] returned from the previous step.
 *   or
 * 4b. Call [showWithActivity] on the [CustomBottomSheetFragment] returned from the previous step.
 * 5.  Override [CloseActionListenerProvider.provide] and return a [CloseActionListener] for the given tag.
 *
 */

enum class BottomSheetType {
    ItemDetail,
    ToteScan,
    ItemComplete,
    QuantityPicker,
    CollectToteLaels,
    AttachToteLabels,
    SubstitutionConfirmation,
    ScanItem,
    ActionSheet,
    ManualEntryStaging,
    UnPick,
    ConfirmAmount,
    BulkSubstitution,
    ToteEstimate,
    MissingItemLocation,
    WhereToFindLocationCode,
    ManualEntryDestaging,
    ManualEntryMfcDestaging,
    AuthCodeVerification,
    ManualEntryToolTip,
    ManualEntryPharmacy,
    HandOffRemoveItems,
}

interface BaseBottomSheetDialogInterface {
    fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding
}

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment(), BaseBottomSheetDialogInterface {
    lateinit var argData: CustomBottomSheetArgData
    val fragmentViewModel: BottomSheetDialogViewModel by viewModel()
    val sharedViewModel: MainActivityViewModel by sharedViewModel() // TODO: ACURED_REDESIGN need to check other alternative to pass the data from fragment to bottomsheet
    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<View>
    fun isArgDataInitialized() = ::argData.isInitialized

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        argData = requireArguments().getParcelable<CustomBottomSheetArgData>(BUNDLE_KEY_ARG_DATA) as CustomBottomSheetArgData
        val binding = getViewDataBinding(inflater, container)
        binding.lifecycleOwner = this.viewLifecycleOwner
        handleClickListeners()
        handleSnackbarListener()
        return binding.root
    }

    private fun handleSnackbarListener() {
        sharedViewModel.snackEvent.observe(viewLifecycleOwner) {
            AcupickSnackbar.make(this, it).show()
        }
    }

    private fun handleClickListeners() {
        fragmentViewModel.navigation.observe(viewLifecycleOwner) { closeAction ->
            Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
            // ACIP-256500 -> Checking if the fragment is already added to the fragment manager and not in the saved state
            dismissBottomSheet()
            // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
            findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listPair = listOf(Pair(EventKey.BOTTOM_SHEET_NAME, argData.dialogType.name))
        fragmentViewModel.firebaseAnalytics.logEvent(EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_OPEN, listPair)

        dialog?.setCanceledOnTouchOutside(argData.cancelOnTouchOutside)
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog
            bottomSheetDialog?.dismissWithAnimation = true
            val bottomSheet = bottomSheetDialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                standardBottomSheetBehavior = BottomSheetBehavior.from(it)
                standardBottomSheetBehavior.isDraggable = argData.draggable
                // it.layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT // TODO: ACURED_REDESIGN Once Validate all the bottomsheet in QA process will remove this
                if (argData.isFullScreen) {
                    standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    checkIfFragmentAttached {
                        standardBottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(argData.peekHeight)
                    }
                }
            }
        }
    }

    private fun checkIfFragmentAttached(operation: Context.() -> Unit) {
        if (isAdded && context != null) {
            operation(requireContext())
        }
    }

    fun dismissBottomSheet() {
        // ACIP-256500 -> Checking if the fragment is already added to the fragment manager and not in the saved state
        if (isAdded && !isDetached) {
            if (!isStateSaved) {
                dismiss()
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentViewModel.firebaseAnalytics.logEvent(
            EventCategory.BOTTOM_SHEET, EventAction.SCREEN_VIEW, EventLabel.BOTTOM_SHEET_STATE_CLOSE,
            if (isArgDataInitialized()) listOf(Pair(EventKey.BOTTOM_SHEET_NAME, argData.dialogType.name)) else null
        )
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Timber.v("[onCancel] sending CloseAction.Dismiss")
        findDialogListener()?.onCloseAction(CloseAction.Dismiss, null)
    }

    abstract override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding

    companion object {
        private const val BUNDLE_KEY_ARG_DATA = "argData"

        fun newInstance(argData: CustomBottomSheetArgData): BaseBottomSheetDialogFragment {
            return when (argData.dialogType) {
                BottomSheetType.ItemDetail -> ItemDetailBottomSheetFragment()
                BottomSheetType.ToteScan -> ToteBottomSheetFragment()
                BottomSheetType.QuantityPicker -> QuantityPickerBottomSheet()
                BottomSheetType.ItemComplete -> ToteBottomSheetFragment()
                BottomSheetType.CollectToteLaels -> CollectToteLabelBottomSheet()
                BottomSheetType.AttachToteLabels -> AttachToteLabelBottomSheet()
                BottomSheetType.SubstitutionConfirmation -> SubstituteConfirmationBottomSheet()
                BottomSheetType.BulkSubstitution -> BulkSubstituteBottomSheet()
                BottomSheetType.ScanItem -> ScanItemBottomSheet()
                BottomSheetType.ActionSheet -> ActionSheetBottomSheet()
                BottomSheetType.ManualEntryStaging -> ManualEntryStagingBottomSheet()
                BottomSheetType.UnPick -> UnPickBottomSheet()
                BottomSheetType.ConfirmAmount -> ConfirmAmountBottomSheet()
                BottomSheetType.ToteEstimate -> ToteEstimateBottomSheet()
                BottomSheetType.MissingItemLocation -> MissingItemLocationBottomSheet()
                BottomSheetType.WhereToFindLocationCode -> WhereToFindLocationCodeBottomSheet()
                BottomSheetType.ManualEntryDestaging -> ManualEntryDestagingBottomSheet()
                BottomSheetType.ManualEntryMfcDestaging -> ManualEntryDestagingMfcBottomSheet()
                BottomSheetType.AuthCodeVerification -> AuthCodeVerificationBottomSheet()
                BottomSheetType.ManualEntryToolTip -> ManualEntryToolTipBottomSheet()
                BottomSheetType.ManualEntryPharmacy -> ManualEntryPharmacyBottomSheet()
                BottomSheetType.HandOffRemoveItems -> HandOffRemoveItemsBottomSheet()
            }.apply {
                arguments = Bundle().apply {
                    putParcelable(BUNDLE_KEY_ARG_DATA, argData)
                }
            }
        }
    }
}
