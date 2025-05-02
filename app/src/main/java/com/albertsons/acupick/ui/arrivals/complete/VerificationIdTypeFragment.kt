package com.albertsons.acupick.ui.arrivals.complete

import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.databinding.VerificationIdTypeFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.models.IdentificationInfo
import com.albertsons.acupick.ui.models.IdentificationType
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.inject

class VerificationIdTypeFragment : BaseFragment<VerificationIdTypeViewModel, VerificationIdTypeFragmentBinding>() {

    override val fragmentViewModel: VerificationIdTypeViewModel by viewModel()
    val sharedViewModel: HandOffVerificationSharedViewModel by navGraphViewModels(R.id.handOffScope)
    private val args: VerificationIdTypeFragmentArgs by navArgs()
    val siteRepo: SiteRepository by inject()

    override fun getLayoutRes(): Int = R.layout.verification_id_type_fragment

    override fun setupBinding(binding: VerificationIdTypeFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentLifecycleOwner = viewLifecycleOwner
        binding.viewModel = fragmentViewModel

        activityViewModel.setToolbarTitle(getString(R.string.verification))

        with(fragmentViewModel) {
            orderNumber.value = args.orderNumber
            isDugOrder.value = sharedViewModel.orderInfoMap[orderNumber.value]?.isDugOrder ?: false

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                onContinueClickedEvent.collect { (orderNumber, selection) ->
                    sharedViewModel.orderInfoMap[orderNumber]?.identificationInfo =
                        IdentificationInfo(identificationType = selection, name = null, dateOfBirth = null, identificationNumber = null, pickupPersonSignature = null)
                    findNavController().navigate(
                        when (selection) {
                            IdentificationType.DriversLicense ->
                                if (siteRepo.isAgeVerificationCameraEnabled) {
                                    VerificationIdTypeFragmentDirections.actionToIdentificationBarcodeScanFragment(orderNumber)
                                } else {
                                    VerificationIdTypeFragmentDirections.actionToVerificationManualEntryFragment(orderNumber)
                                }
                            else -> VerificationIdTypeFragmentDirections.actionToVerificationManualEntryFragment(orderNumber)
                        }
                    )
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                onIdUnavailableClickedEvent.collect { orderNumber ->
                    sharedViewModel.idUnavailableEvent.emit(orderNumber)
                    findNavController().popBackStack()
                }
            }
        }
    }
}
