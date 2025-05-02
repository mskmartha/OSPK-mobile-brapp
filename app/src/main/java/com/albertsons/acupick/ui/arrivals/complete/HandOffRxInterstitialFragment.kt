package com.albertsons.acupick.ui.arrivals.complete

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.databinding.HandOffRxInterstitialFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class HandOffRxInterstitialFragment : BaseFragment<HandOffRxInterstitialViewModel, HandOffRxInterstitialFragmentBinding>() {

    override val fragmentViewModel: HandOffRxInterstitialViewModel by stateViewModel()
    private val args: HandOffRxInterstitialFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Block back button and do nothing here
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentViewModel.closeAction.observe(viewLifecycleOwner) {
            if (it) findNavController().popBackStack()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getLayoutRes(): Int = R.layout.hand_off_rx_interstitial_fragment

    override fun setupBinding(binding: HandOffRxInterstitialFragmentBinding) {
        super.setupBinding(binding)
        with(fragmentViewModel) {
            scannedBags = args.pickedBagNumbers
            orderSummaryParamsList = args.orderSummaryParamsList.list
        }
        fragmentViewModel.handleHandoffCompletion(args.handOffInterstitialParamsList, args.handOffAction)
        binding.isHandOffActionCancel = fragmentViewModel.handOffAction.value == HandOffAction.CANCEL
    }
}
