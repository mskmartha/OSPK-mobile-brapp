package com.albertsons.acupick.ui.arrivals.complete

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.CustomerSignatureFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerSignatureFragment : BaseFragment<CustomerSignatureViewModel, CustomerSignatureFragmentBinding>() {
    val sharedViewModel: HandOffVerificationSharedViewModel by navGraphViewModels(R.id.handOffScope)
    override val fragmentViewModel: CustomerSignatureViewModel by viewModel()

    override fun getLayoutRes() = R.layout.customer_signature_fragment

    private val args: CustomerSignatureFragmentArgs by navArgs()

    override fun setupBinding(binding: CustomerSignatureFragmentBinding) {
        super.setupBinding(binding)

        with(fragmentViewModel) {
            orderNumber.value = args.orderNumber

            viewLifecycleOwner.lifecycleScope.launch {
                getSignatureFlow(binding.signaturePad)
                    .collect { signatureStarted ->
                        signatureButtonsEnabled.value = signatureStarted
                    }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                clearSignatueEvent.collect {
                    binding.signaturePad.clear()
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                saveSignatueEvent.collect {
                    encodeSignatureToString(binding.signaturePad.signatureBitmap)
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                signatureString.collect { signature ->
                    if (signature != null) {
                        sharedViewModel.orderInfoMap[args.orderNumber]?.identificationInfo?.pickupPersonSignature = signature
                        sharedViewModel.pickupPersonDataCompleteEvent.emit(args.orderNumber)
                        findNavController().popBackStack(R.id.handOffFragment, false)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(activityViewModel) {
            emptyToolbar()
            setToolbarTitle(context?.getString(R.string.toolbar_title_signature) ?: "")
            setToolbarNavigationIcon(context?.getDrawable(R.drawable.ic_back_arrow))
            setToolBarVisibility(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
