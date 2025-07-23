package com.albertsons.acupick.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.albertsons.acupick.ui.dialog.firstlaunch.FirstLaunchDialogFragment
import com.albertsons.acupick.ui.util.UserFeedback
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

/**
 * Abstract custom dialog class that displays data (via [ArgData]) styled for the app. Handles dialog [CloseAction] via [CloseActionListenerProvider] on host fragment/activity/[CloseActionListener]
 * for each dialog displayed by the fragment/activity.
 *
 * ### How to use:
 * 1.  Extend this class, overriding [getViewDataBinding].
 * 2.  Make the host fragment/activity implement [CloseActionListenerProvider].
 * 3.  Call [CustomDialogFragment.newInstance] with data to be shown in the dialog.
 * 4a. Call [showWithFragment] on the [CustomDialogFragment] returned from the previous step.
 *   or
 * 4b. Call [showWithActivity] on the [CustomDialogFragment] returned from the previous step.
 * 5.  Override [CloseActionListenerProvider.provide] and return a [CloseActionListener] for the given tag.
 *
 * ### Notes
 * Note that click listener lambdas **CANNOT** be used in [CustomDialogArgData] due to function lambdas not being serializable!
 * Quick way to test: trigger via dialog show -> background -> process death (device/emu OS Settings -> Dev Options -> Background Process Limit: No Background Processes) -> observe crash.
 * This approach separates the listener from the data and allows proper OS re-instantiation from saved instance state while also maintaining dialog CTA handling, preventing that type of crash.
 */

enum class DialogType {
    AlternativeLocations,
    ConfirmSameItem,
    ConfirmItem,
    ConfirmIssueScanItem,
    DestagingDialog,
    HybridNotification,
    CustomHybridNotification,
    Informational,
    DriverIdVerification,
    ItemDetails,
    OrderedByWeight,
    QuantityPicker,
    RadioButtons,
    CustomRadioButtons,
    SubbedItemAlert,
    EbtWarning,
    AuthCodeError,
    PharmacyStaffRequired,
    ModalFiveConfirmation,
    OnePlUnwantedItemRemovalConfirmation,
    RemoveSubstitution,
    EbtNoBagsWarning,
    TitleImageInfo,
    InterjectionForALLUsers,
    InformationDialog,
    OnePlGiftingDialog,
    FirstLaunchDialogFragment
}

enum class DialogStyle {
    OfAgeVerification,
    DriverIdVerification,
    RejectedItems,
    PrintShippingLabel
}

interface BaseCustomDialogInterface {
    fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding
}

abstract class BaseCustomDialogFragment : DialogFragment(), BaseCustomDialogInterface {

    lateinit var argData: CustomDialogArgData
    val fragmentViewModel: CustomDialogViewModel by viewModel()
    open val shouldFillScreen: Boolean = true
    private val userFeedback by inject<UserFeedback>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        argData = requireArguments().getSerializable(BUNDLE_KEY_ARG_DATA) as CustomDialogArgData

        val binding = getViewDataBinding(inflater, container)

        binding.lifecycleOwner = this.viewLifecycleOwner
        setupBinding()
        isCancelable = argData.cancelable

        requireDialog().apply {
            setCanceledOnTouchOutside(argData.cancelOnTouchOutside)
        }
        argData.soundAndHaptic?.let {
            when (it) {
                UserFeedback.SoundAndHaptic.ArrivalInterjection.shortName -> {
                    userFeedback.setInterjectionsSoundAndHaptic()
                }
                UserFeedback.SoundAndHaptic.PickingInterjection.shortName -> {
                    userFeedback.setPickingInterjectionsSoundAndHaptic()
                }
            }
        }
        return binding.root
    }

    abstract override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Timber.v("[onCancel] sending CloseAction.Dismiss")
        findDialogListener()?.onCloseAction(CloseAction.Dismiss, null)
    }

    override fun onResume() {
        super.onResume()
        // Forces the dialog width to match_parent to fill device width. If not used, the dialog would render in a wrap_content sort of way even if the root layout used match_parent.
        // See https://stackoverflow.com/questions/23990726/how-to-make-dialogfragment-width-to-fill-parent#comment58917595_26207535
        if (shouldFillScreen) {
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun setupBinding() {
        fragmentViewModel.navigation.observe(viewLifecycleOwner) { closeAction ->
            Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
            dismiss()
            // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
            findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
        }
        fragmentViewModel.navigationWithData.observe(viewLifecycleOwner) { closeAction ->
            Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
            dismiss()
            // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
            findDialogListener()?.onCloseActionWithData(closeAction.first, closeAction.second)
        }
    }

    companion object {
        private const val BUNDLE_KEY_ARG_DATA = "argData"

        fun newInstance(argData: CustomDialogArgData): BaseCustomDialogFragment {
            return when (argData.dialogType) {
                DialogType.Informational -> InformationalDialogFragment()
                DialogType.RadioButtons -> RadioButtonsDialogFragment()
                DialogType.CustomRadioButtons -> CustomRadioButtonsDialogFragment()
                DialogType.ItemDetails -> ItemDetailsDialogFragment()
                DialogType.SubbedItemAlert -> SubbedItemAlertDialogFragment()
                DialogType.QuantityPicker -> QuantityPickerDialogFragment()
                DialogType.ConfirmSameItem -> ConfirmSameItemDialogFragment()
                DialogType.ConfirmItem -> ConfirmItemDialogFragment()
                DialogType.ConfirmIssueScanItem -> ConfirmIssueScanItemDialogFragment()
                DialogType.AlternativeLocations -> AlternativeLocationDialogFragment()
                DialogType.DestagingDialog -> DestagingDialogFragment()
                DialogType.HybridNotification -> HybridNotificationDialogFragment()
                DialogType.CustomHybridNotification -> CustomHybridNotificationDialogFragment()
                DialogType.PharmacyStaffRequired -> PharmacyStaffRequiredDialogFragment()
                DialogType.EbtWarning, DialogType.EbtNoBagsWarning -> WarningDialogFragment()
                DialogType.AuthCodeError -> AuthCodeInvalidDialog()
                DialogType.OrderedByWeight -> OrderedByWeightDialogFragment()
                DialogType.DriverIdVerification -> DriverIdVerificationDialogFragment()
                DialogType.ModalFiveConfirmation -> Modal5ConfirmationDialogFragment()
                DialogType.OnePlUnwantedItemRemovalConfirmation -> OnePlInfoDialogFragment()
                DialogType.RemoveSubstitution -> RemoveSubstitutionDialogFragment()
                DialogType.TitleImageInfo -> TitleImageInfoDialogFragment()
                DialogType.InterjectionForALLUsers -> InterjectionForAllUsersDialogFragment()
                DialogType.InformationDialog -> InformationDialogFragment()
                DialogType.OnePlGiftingDialog -> OnePlGiftingDialogFragment()
                DialogType.FirstLaunchDialogFragment -> FirstLaunchDialogFragment()
            }.apply {
                arguments = Bundle().apply {
                    putSerializable(BUNDLE_KEY_ARG_DATA, argData)
                }
            }
        }
    }
}
