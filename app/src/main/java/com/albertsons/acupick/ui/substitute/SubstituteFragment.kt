package com.albertsons.acupick.ui.substitute

import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.databinding.SubstituteFragmentBinding
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.MainActivityViewModel.Companion.ERROR_DIALOG_TAG
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.CloseActionListener
import com.albertsons.acupick.ui.itemdetails.ItemDetailsViewModel
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPagerViewModel.Companion.MANUAL_ENTRY_PICK_RESULTS
import com.albertsons.acupick.ui.manualentry.pick.ManualEntryPickResults
import com.albertsons.acupick.ui.models.AcupickSnackEvent
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.REMOVE_SUBSTITUTION_REQUEST_KEY
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.REMOVE_SUBSTITUTION_RESULTS
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.SUBSTITUTION_SUGGESTED_ITEM_DETAILS
import com.albertsons.acupick.ui.util.AcupickSnackbar
import com.albertsons.acupick.ui.util.SnackType
import com.albertsons.acupick.ui.util.StringIdHelper
import com.albertsons.acupick.ui.util.UserFeedback
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class SubstituteFragment : BaseFragment<SubstituteViewModel, SubstituteFragmentBinding>() {
    // DI
    private val userFeedback by inject<UserFeedback>()

    // fragmentViewModel
    override val fragmentViewModel: SubstituteViewModel by stateViewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes() = R.layout.substitute_fragment

    private val args: SubstituteFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackOrUpButton()
        }
    }

    override fun setupBinding(binding: SubstituteFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner

        fragmentViewModel.pickListId = args.substituteParams.pickListId
        args.substituteParams.iaId?.let {
            fragmentViewModel.iaIdBeforeSubstitution.postValue(it)
        }
        fragmentViewModel.swapSubReason.postValue(args.substituteParams.swapSubstitutionReason)
        args.substituteParams.messageSid?.let {
            fragmentViewModel.messageSid.postValue(it)
        }
        args.substituteParams.path?.let {
            fragmentViewModel.substitutionPath.postValue(it)
        }

        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            })
        }
        when (args.substituteParams.path) {
            SubstitutionPath.SWAPSUBSTITUTION, SubstitutionPath.REPICK_ORIGINAL_ITEM -> {
                fragmentViewModel.isFromSwapSubstitution.postValue(true)
                args.substituteParams.substitutionRemovedQty?.let {
                    fragmentViewModel.showSnackBar(AcupickSnackEvent(message = StringIdHelper.Plural(R.plurals.items_removed_from_substitution, it), SnackType.SUCCESS))
                }
            }
            else -> Unit
        }

        activityViewModel.scannedData.observe(viewLifecycleOwner) {
            fragmentViewModel.onScannerBarcodeReceived(it)
        }

        fragmentViewModel.playScanSound.observe(viewLifecycleOwner) { isSuccess ->
            when (isSuccess) {
                true -> userFeedback.setSuccessScannedSoundAndHaptic()
                false -> userFeedback.setFailureScannedSoundAndHaptic()
            }
        }

        setFragmentResultListener(ItemDetailsViewModel.ITEM_DETAILS_PLU_REQUEST_KEY) { _, _ ->
            fragmentViewModel.handlePluCtaResult()
        }

        fragmentViewModel.snackBarMessageOnNavigateUp.observe(viewLifecycleOwner) { message ->
            findNavController().previousBackStackEntry?.savedStateHandle?.set(SubstituteViewModel.KEY_ITEM_HAS_UPDATED, message)
        }

        activityViewModel.navigationButtonIntercept.observe(viewLifecycleOwner) {
            handleBackOrUpButton()
        }

        activityViewModel.bottomSheetBackButtonPressed.observe(viewLifecycleOwner) {
            handleBackOrUpButton()
        }

        fragmentViewModel.acupickSnackEventAnchored.observe(viewLifecycleOwner) {
            AcupickSnackbar.make(this@SubstituteFragment, it)
                .setAnchorView(binding.scanItemTv)
                .show()
        }

        setUpFragmentResultListeners()
    }

    private fun setUpFragmentResultListeners() {
        requireActivity().supportFragmentManager.setFragmentResultListener(MANUAL_ENTRY_PICK, this) { _, bundle ->
            Timber.d("1292 MANUAL_ENTRY_PICK listener")
            // Any type can be passed via to the bundle
            val manualEntryResults = bundle.get(MANUAL_ENTRY_PICK_RESULTS)
            (manualEntryResults as? ManualEntryPickResults)?.let { results ->
                fragmentViewModel.setIsFromManualEntry(true)
                fragmentViewModel.lastScannedItem = results.itemDetails
                fragmentViewModel.weight.value = results.weightEntry
                fragmentViewModel.lastItemBarcodeScanned = results.barcode as? BarcodeType.Item
                if (fragmentViewModel.bulkVariants.isNotNullOrEmpty()) {
                    results.barcode?.let { (it as? BarcodeType)?.let { it1 -> fragmentViewModel.onScannerBarcodeReceived(it1) } }
                } else {
                    fragmentViewModel.handleManualEntryData(results.quantity.toQuantity(), results.barcode as? BarcodeType.Item)
                }
            }
        }

        setFragmentResultListener(ManualEntryPagerViewModel.BYPASS_QUANTITY_PICKER) { _, bundle ->
            val serializableResult = bundle.get(ManualEntryPagerViewModel.BARCODE_TYPE)
            (serializableResult as BarcodeType).let {
                fragmentViewModel.setIsFromManualEntry(true)
                fragmentViewModel.onScannerBarcodeReceived(it)
            }
        }

        setFragmentResultListener(REMOVE_SUBSTITUTION_REQUEST_KEY) { _, bundle ->
            val itemRemove = bundle.get(REMOVE_SUBSTITUTION_RESULTS)
            fragmentViewModel.showDeleteSubItemDialog(itemRemove as SubstitutionLocalItem)
        }
    }

    override fun provide(tag: String?): CloseActionListener? {
        return when (tag) {
            ERROR_DIALOG_TAG -> errorDialogListener
            SUBSTITUTION_SUGGESTED_ITEM_DETAILS -> closeDialogListener
            else -> super.provide(tag) ?: run {
                Timber.e("[provide] unhandled dialog listener")
                null
            }
        }
    }

    private fun handleBackOrUpButton() {
        with(fragmentViewModel) {
            when {
                isSomethingSubstituted() -> showExitSubstitutionDialog() // Exit from normal substitution flow if picker has scanned any item
                shouldShowExitSwapSubstitionDialog() || shouldShowExitSwapSubstitutionDialogForOtherPicker() -> showExitSwapSubstitutionDialog() // Exit from swap sub flow for my items
                validateSwapSubstitutionForOtherPicker() -> exitFromSwapSubstitutionForOtherPickersItem() // Exit from swap sub flow for other picker's item
                else -> {
                    // Exit from normal substitution flow
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(SubstituteViewModel.NAVIGATE_BACK_FROM_SUBSTITUTION_UI, true)
                    findNavController().navigateUp()
                }
            }
        }
    }

    private val errorDialogListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            // ToDo determine what calls to attach to as no initial api call to refresh state to
            when (closeAction) {
                CloseAction.Positive,
                CloseAction.Negative,
                CloseAction.Dismiss -> Any()
            }.exhaustive
        }
    }

    private val closeDialogListener = object : CloseActionListener {
        override fun onCloseAction(closeAction: CloseAction, result: Int?) {
            Timber.v("[onCloseAction] closeAction=$closeAction")
            fragmentViewModel.canAcceptScan = true
        }
    }
}
