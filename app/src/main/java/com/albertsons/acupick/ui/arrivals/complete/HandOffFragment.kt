package com.albertsons.acupick.ui.arrivals.complete

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.logic.HandOffVerificationState.ITEMS_REMOVED
import com.albertsons.acupick.data.logic.HandOffVerificationState.REMOVE_ITEMS
import com.albertsons.acupick.databinding.HandOffFragmentBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.BOTTOM_SHEET_DIALOG_TAG
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.HANDOFF_REMOVE_ITEMS_REQUEST_KEY
import com.albertsons.acupick.ui.dialog.showWithFragment
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.ZonedDateTime

class HandOffFragment : BaseFragment<HandOffViewModel, HandOffFragmentBinding>() {

    private val args: HandOffPagerFragmentArgs by navArgs()

    override val fragmentViewModel: HandOffViewModel by viewModel {
        parametersOf(args.handOffArgData.currentHandOffUI, getSharedViewModel<MainActivityViewModel>())
    }

    // Pager VM for observing data updates.
    private val pagerVM: HandOffPagerViewModel by navGraphViewModels(R.id.handOffScope)

    val sharedViewModel: HandOffVerificationSharedViewModel by navGraphViewModels(R.id.handOffScope)

    override fun getLayoutRes(): Int = R.layout.hand_off_fragment

    override fun setupBinding(binding: HandOffFragmentBinding) {
        super.setupBinding(binding)

        binding.fragmentLifecycleOwner = viewLifecycleOwner
        with(fragmentViewModel) {
            // Initialize handoffUI
            setHandOffUi(args.handOffArgData.currentHandOffUI)
            handOffUI.postValue(args.handOffArgData.currentHandOffUI)
            customerIndex.postValue(args.handOffArgData.currentHandOffIndex + 1)
            customerCount.postValue(args.handOffArgData.handOffUIList.size)
            isAuthDugEnabled.postValue(args.handOffArgData.currentHandOffUI?.isAuthDugEnabled)
            handOffResultFlow.value = args.handOffArgData.currentHandOffResultData
            isFromPartialPrescriptionReturn.postValue(args.isFromPartialPrescriptionReturn)
            // args.pickedBagNumbers?.let { setPickedBagNumbers(it.scannedData) }

            // get IdentificationInfo from sharedViewModel
            handOffUI.value?.orderNumber?.let { orderNumber ->
                sharedViewModel.orderInfoMap[orderNumber]?.identificationInfo?.let { idInfo ->
                    identificationInfo.value = idInfo
                }
            }

            // Checking the checkbox triggers the bottom sheet opening, with the complete CTA enabled
            shouldTriggerBottomSheet.observe(viewLifecycleOwner) { isComplete ->
                if (isDugOrder.value == true && isComplete) {
                    HandOffBottomSheetDialogFragment.newInstance(true).showWithFragment(this@HandOffFragment, BOTTOM_SHEET_DIALOG_TAG)
                }
            }

            expandBottomSheetAction.observe(viewLifecycleOwner) {
                HandOffBottomSheetDialogFragment.newInstance(shouldTriggerBottomSheet.value ?: false).showWithFragment(this@HandOffFragment, BOTTOM_SHEET_DIALOG_TAG)
            }
            handOffUI.observe(viewLifecycleOwner) {
                pagerVM.updateCurrentHandoffUi(it)
            }
            rxOrderStatus.observe(viewLifecycleOwner) {
                pagerVM.updateRxOrderStatus(it)
            }
            // pagerVM.isFromPartialPrescriptionReturn.observe(viewLifecycleOwner) {
            //     isFromPartialPrescriptionReturn.postValue(it)
            // }

            codeVerifiedOrReportLogged.observe(viewLifecycleOwner) { isVerifiedOrReported ->
                if (isVerifiedOrReported == true) {
                    setUpHeader()
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                handOffResultFlow.collect { resultData ->
                    resultData?.let { nonNullResultData ->
                        args.handOffArgData.currentHandOffUI?.let { handOffUI ->
                            pagerVM.updateHandOffState(
                                handOffUI, nonNullResultData
                            )
                            authCodeIssueReported.postValue(resultData.authCodeUnavailableReasonCode != null)
                        }
                    }
                }
            }

            passRemovedItems.observe(viewLifecycleOwner) {
                pagerVM.updateRejectedItems(it)
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                sharedViewModel.idUnavailableEvent.collectLatest { orderNumber ->
                    if (orderNumber == handOffUI.value?.orderNumber) {
                        pagerVM.switchToOrder(orderNumber)
                        fragmentViewModel.identificationInfo.value = null
                        if (sharedViewModel.itemsRemoved[handOffUI.value?.orderNumber] == true) {
                            handOffVerificationState.postValue(ITEMS_REMOVED)
                        } else {
                            handOffVerificationState.postValue(REMOVE_ITEMS)
                        }
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                sharedViewModel.pickupPersonDataCompleteEvent.collectLatest { orderNumber ->
                    pagerVM.switchToOrder(orderNumber)
                    if (orderNumber == args.handOffArgData.currentHandOffUI?.orderNumber) {
                        val data = sharedViewModel.orderInfoMap[orderNumber]?.identificationInfo
                        acuPickLogger.i(
                            "pickupPersonDataCompleteEvent: order: $orderNumber, hasPickPersonData: ${data != null} " +
                                "hasPickName: ${data?.name.isNotNullOrEmpty()} hasName: ${data?.dateOfBirth != null}, hasId: ${data?.identificationNumber.isNotNullOrEmpty()}"
                        )
                        args.handOffArgData.currentHandOffResultData?.isIdVerified = true
                        confirmVerification(data)
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                storeVerificationInfo.collect { (orderNumber, verificationInfo) ->
                    sharedViewModel.orderInfoMap[orderNumber] = verificationInfo
                    findNavController().navigate(
                        HandOffPagerFragmentDirections.actionHandOffFragmentToVerificationIdTypeFragment(orderNumber)
                    )
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                clearVerificationInfoEvent.collect { orderNumber ->
                    // remove the id information if available from the shared preferences if the user clicks on the id unavailable button
                    fragmentViewModel.cleanUpIdVerification()
                    sharedViewModel.orderInfoMap.remove(orderNumber)
                }
            }

            setUpStatePersistance()

            // DUG interjection set dug interjection state during handoff flow
            setDugInterjectionState(DugInterjectionState.HandoffInProgress)

            childFragmentManager.setFragmentResultListener(HandOffViewModel.HANDOFF_AUTH_CODE_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
                val authCode = bundle.getString(HandOffViewModel.AUTH_CODE_VALIDATION_RESULTS)
                val codeUnavailable = bundle.getBoolean(HandOffViewModel.AUTH_CODE_UNAVAILABLE)
                if (codeUnavailable) {
                    onAuthCodeUnavailableCtaClicked()
                } else {
                    verificationCodeEntryText.value = authCode
                    onCompleteClicked()
                }
                // isAuthCodeVerified.postValue(true)
            }
            childFragmentManager.setFragmentResultListener(HANDOFF_REMOVE_ITEMS_REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
                val removedItems = bundle.getBoolean(HandOffViewModel.HANDOFF_REMOVED_ITEMS)
                if (removedItems) {
                    onRemoveItemsConfirmed()
                } else {
                    onItemsRemoveCancelled()
                }
            }
        }

        with(pagerVM) {
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                restageOrderByOrderNumberEvent.collect { orderNumber ->
                    fragmentViewModel.apply {
                        if (shouldHandleRestageOrder(orderNumber)) {
                            launchRestageDialog()
                        }
                    }
                }
            }
        }
        activityViewModel.setToolbarNavigationIcon(null)

        pagerVM.activeOrderNumber.observe(viewLifecycleOwner, fragmentViewModel::onChangeActiveOrder)

        // fragmentViewModel.scroller.observe(viewLifecycleOwner) { isScroll ->
        //     viewLifecycleOwner.lifecycleScope.launch {
        //         delay(1000)
        //         if (isScroll && binding.authCodeUnavailableCta.isShown) {
        //             binding.topView.handoffTopView.smoothScrollToPosition(2)
        //         }
        //     }
        // }

        collectFlow(pagerVM.exitHandOffEvent) {
            fragmentViewModel.onExitHandOffEvent(it.first, it.second)
        }
    }

    /*
       Due to the HandOffPagerFragment and viewPager2 adapter being destroyed when navigatating from the HandOffFragment,
       we store the hand off state values on the HandOffSharedViewModel by the order number and post it back when the fragment isCreated
    * */
    private fun HandOffViewModel.setUpStatePersistance() {
        val currentOrderNumber = args.handOffArgData.currentHandOffUI?.orderNumber ?: ""

        // HandOffVerificationState - current state of the current order in the handOff flow
        sharedViewModel.handOffVerificationStateMap[currentOrderNumber]?.let {
            handOffVerificationState.postValue(it)
        }

        handOffVerificationState.observe(viewLifecycleOwner) { handOffState ->
            sharedViewModel.handOffVerificationStateMap[currentOrderNumber] = handOffState
        }

        // HandOffResultData - values used in /pickupComplete call
        handOffResultFlow.value = sharedViewModel.handOffResultDataMap[currentOrderNumber]

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            handOffResultFlow.collect { resultData ->
                sharedViewModel.handOffResultDataMap[currentOrderNumber] = resultData
            }
        }

        // Auth Code Verified - shows the report logged textview
        authCodeIssueReported.postValue(
            sharedViewModel.authCodeIssueReportedMap[currentOrderNumber]
        )

        authCodeIssueReported.observe(viewLifecycleOwner) { issueReported ->
            sharedViewModel.authCodeIssueReportedMap[currentOrderNumber] = issueReported ?: false
        }

        isAuthCodeVerified.postValue(
            sharedViewModel.authCodeVerifiedMap[currentOrderNumber]
        )

        isAuthCodeVerified.observe(viewLifecycleOwner) { isVerified ->
            sharedViewModel.authCodeVerifiedMap[currentOrderNumber] = isVerified ?: false
        }

        isConfirmOrderChecked.postValue(
            sharedViewModel.checkMarkCheckedMap[currentOrderNumber] ?: false
        )

        isConfirmOrderChecked.observe(viewLifecycleOwner) { isChecked ->
            sharedViewModel.checkMarkCheckedMap[currentOrderNumber] = isChecked ?: false
            if (isChecked == true) {
                updateOtpByPassTimeStamp(ZonedDateTime.now())
            }
        }

        restrictedItemRemoved.postValue(
            sharedViewModel.itemsRemoved[currentOrderNumber] ?: false
        )

        restrictedItemRemoved.observe(viewLifecycleOwner) { itemRemoved ->
            sharedViewModel.itemsRemoved[currentOrderNumber] = itemRemoved ?: false
        }

        // this is needed to enable the Rx complete button when the auth code is verified or issue is reported
        removeItemsCtaEnabled.postValue(
            sharedViewModel.removeItemsCtaEnabledMap[currentOrderNumber] ?: false
        )

        removeItemsCtaEnabled.observe(viewLifecycleOwner) { ctaEnabled ->
            sharedViewModel.removeItemsCtaEnabledMap[currentOrderNumber] = ctaEnabled ?: false
        }

        // isIdVerified - has the pickup persons id been checked
        isIdVerified.value = sharedViewModel.idVerifiedMap[currentOrderNumber].orFalse()

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            isIdVerified.collect { idVerified ->
                sharedViewModel.idVerifiedMap[currentOrderNumber] = idVerified
            }
        }

        // otpCapturedTimestampFlow.value = sharedViewModel.otpCapturedTimestampMap[currentOrderNumber]
        updateOtpCapturedTimeStamp(sharedViewModel.otpCapturedTimestampMap[currentOrderNumber])

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                otpCapturedTimestampFlow.collect { otpCapturedTimestamp ->
                    sharedViewModel.otpCapturedTimestampMap[currentOrderNumber] = otpCapturedTimestamp
                }
            }
        }

        // otpByPassTimeStampFlow.value = sharedViewModel.otpBypassTimestampMap[currentOrderNumber]
        updateOtpByPassTimeStamp(sharedViewModel.otpBypassTimestampMap[currentOrderNumber])

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                otpByPassTimeStampFlow.collect { otpByPassTimeStamp ->
                    Timber.d("Handoff otpbyPass time stamp collect $otpByPassTimeStamp")
                    sharedViewModel.otpBypassTimestampMap[currentOrderNumber] = otpByPassTimeStamp
                }
            }
        }

        authCodeUnavailableReasonCodeFlow.value = sharedViewModel.authCodeUnavailableReasonCodeMap[currentOrderNumber]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authCodeUnavailableReasonCodeFlow.collect { authCodeUnavailableReasonCode ->
                    sharedViewModel.authCodeUnavailableReasonCodeMap[currentOrderNumber] = authCodeUnavailableReasonCode
                }
            }
        }
    }
}
