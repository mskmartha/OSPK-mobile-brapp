package com.albertsons.acupick.ui.staging.winestaging.weight

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BoxInputWeightFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging2Fragment.Companion.BOX_KEY
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging2Fragment.Companion.REQUEST
import com.albertsons.acupick.ui.staging.winestaging.weight.WineStaging2Fragment.Companion.WEIGHT_KEY
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class BoxInputWeightFragment : BaseFragment<BoxInputWeightViewModel, BoxInputWeightFragmentBinding>() {
    private val args: BoxInputWeightFragmentArgs by navArgs()
    override val fragmentViewModel: BoxInputWeightViewModel by viewModel() {
        parametersOf(args.weight, args.boxLabel)
    }
    override fun getLayoutRes() = R.layout.box_input_weight_fragment

    override fun setupBinding(binding: BoxInputWeightFragmentBinding) {
        super.setupBinding(binding)
        binding.viewModel = fragmentViewModel
        fragmentViewModel.updateScreen(args.boxLabel)

        fragmentViewModel.weightSet.observe(viewLifecycleOwner) {
            setResultAndExit(it.first, it.second)
        }
    }

    private fun setResultAndExit(boxWeight: String, boxLabel: String) {
        val result = Bundle().apply {
            putString(WEIGHT_KEY, boxWeight)
            putString(BOX_KEY, boxLabel)
        }
        setFragmentResult(REQUEST, result)
        fragmentViewModel.navigateUp()
    }
}
