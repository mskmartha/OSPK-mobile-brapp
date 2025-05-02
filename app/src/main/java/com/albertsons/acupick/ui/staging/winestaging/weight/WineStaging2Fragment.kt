package com.albertsons.acupick.ui.staging.winestaging.weight

import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.WineStaging2FragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.staging.winestaging.WineStagingFragmentArgs
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WineStaging2Fragment : BaseFragment<WineStaging2ViewModel, WineStaging2FragmentBinding>() {
    private val args: WineStagingFragmentArgs by navArgs()

    override val fragmentViewModel: WineStaging2ViewModel by viewModel() {
        parametersOf(args.wineStagingParams)
    }

    override fun getLayoutRes() = R.layout.wine_staging2_fragment

    override fun setupBinding(binding: WineStaging2FragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentLifecycleOwner = viewLifecycleOwner
        fragmentViewModel.setupHeader(args.wineStagingParams)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentViewModel.fetchData(args.wineStagingParams)
        setFragmentResultListener(REQUEST) { requestKey: String, bundle: Bundle ->
            val weight = bundle.getString(WEIGHT_KEY)
            val boxlabel = bundle.getString(BOX_KEY)
            fragmentViewModel.updateWeight(weight ?: "", boxlabel)
        }
    }

    companion object {
        const val REQUEST = "100"
        const val WEIGHT_KEY = "103"
        const val BOX_KEY = "105"
    }
}
