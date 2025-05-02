package com.albertsons.acupick.ui.arrivals.complete

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.VerificationManualEntryFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.models.IdentificationType.Other
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class VerificationManualEntryFragment : BaseFragment<VerificationManualEntryViewModel, VerificationManualEntryFragmentBinding>() {

    override val fragmentViewModel: VerificationManualEntryViewModel by viewModel()
    val sharedViewModel: HandOffVerificationSharedViewModel by navGraphViewModels(R.id.handOffScope)
    private val args: VerificationIdTypeFragmentArgs by navArgs()

    override fun getLayoutRes() = R.layout.verification_manual_entry_fragment

    override fun setupBinding(binding: VerificationManualEntryFragmentBinding) {
        super.setupBinding(binding)
        binding.viewModel = fragmentViewModel

        activityViewModel.setToolbarTitle(getString(R.string.verification_title))

        with(fragmentViewModel) {
            orderNumber.value = args.orderNumber
            sharedViewModel.orderInfoMap[orderNumber.value]?.let { orderInfo ->
                isDugOrder.value = orderInfo.isDugOrder
                minimumAgeRequired.value = orderInfo.minimumAgeRequired
                getIdInfoFromSharedVM(orderInfo.identificationInfo)
                idType.value = orderInfo.identificationInfo?.identificationType ?: Other
            }
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                onReportAndRemoveClicked.collectLatest {
                    sharedViewModel.idUnavailableEvent.emit(args.orderNumber)
                    findNavController().popBackStack(destinationId = R.id.handOffFragment, inclusive = false)
                }
            }
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                storeIdentificationInfoEvent.observe(viewLifecycleOwner) { idInfo ->
                    sharedViewModel.orderInfoMap[orderNumber.value]?.identificationInfo = idInfo
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                ageVerificationComplete.collect { ageVerificationComplete ->
                    if (ageVerificationComplete) {
                        sharedViewModel.pickupPersonDataCompleteEvent.emit(args.orderNumber)
                        findNavController().popBackStack(R.id.handOffFragment, false)
                    }
                }
            }

            binding.dobTextInputLayout.textInputEditText.addTextChangedListener(DateTextWatcher(binding.dobTextInputLayout.textInputEditText) { dobEntry.value = it })
            binding.dobTextInputLayout.onFocusChangeListener = GhostTextFocusChangeListener(dobEntry.value, binding.dobTextInputLayout.textInputEditText)
        }
    }
}
