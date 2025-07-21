package com.albertsons.acupick.ui.arrivals.complete

import android.os.Bundle
import androidx.activity.addCallback
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.HandOffAction
import com.albertsons.acupick.databinding.HandOffInterstitialFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class HandOffInterstitialFragment : BaseFragment<HandOffInterstitialViewModel, HandOffInterstitialFragmentBinding>() {

    override val fragmentViewModel: HandOffInterstitialViewModel by stateViewModel()
    private val args: HandOffInterstitialFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Block back button and do nothing here
        }
    }

    override fun getLayoutRes(): Int = R.layout.hand_off_interstitial_fragment

    override fun setupBinding(binding: HandOffInterstitialFragmentBinding) {
        super.setupBinding(binding)
        fragmentViewModel.handleHandoffCompletion(args.handOffInterstitialParamsList,
            args.orderSummaryParamsList, args.isFromNotification)
        binding.isHandOffActionCancel = fragmentViewModel.handOffAction.value == HandOffAction.CANCEL

        val handOffItems = fragmentViewModel.handOffCompletedItems
        if (handOffItems.isNotEmpty()) {
            // since there will be a max of 3 orders to be batched we are using individual UI for each order
            // instead of a list view
            binding.handOffItem1 = handOffItems.getOrNull(0)
            binding.handOffItem2 = handOffItems.getOrNull(1)
            binding.handOffItem3 = handOffItems.getOrNull(2)
        }
    }
}
