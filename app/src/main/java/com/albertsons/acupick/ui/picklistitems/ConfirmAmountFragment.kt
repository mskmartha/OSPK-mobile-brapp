package com.albertsons.acupick.ui.picklistitems

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ConfirmAmountFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ConfirmAmountFragment : BaseFragment<ConfirmAmountViewModel, ConfirmAmountFragmentBinding>() {

    companion object {
        const val CONFIRM_AMOUNT_REQUEST = "CONFIRM_AMOUNT_REQUEST"
        const val CONFIRM_AMOUNT_REQUEST_RESULT = "CONFIRM_AMOUNT_REQUEST_RESULT"
    }

    override val fragmentViewModel: ConfirmAmountViewModel by viewModel {
        parametersOf(args.uiData.requestedAmount)
    }

    override fun getLayoutRes(): Int = R.layout.confirm_amount_fragment

    private val args: ConfirmAmountFragmentArgs by navArgs()

    override fun setupBinding(binding: ConfirmAmountFragmentBinding) {
        super.setupBinding(binding)

        activityViewModel.setToolbarTitle(args.uiData.pageTitle.getString(requireContext()))

        binding.apply {
            viewModel = fragmentViewModel
            uiData = args.uiData
            lifecycleOwner = viewLifecycleOwner
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            fragmentViewModel.apply {
                resultNetWeightSharedFlow.collect { resultNetWeightCount ->
                    setFragmentResult(
                        CONFIRM_AMOUNT_REQUEST,
                        bundleOf(
                            CONFIRM_AMOUNT_REQUEST_RESULT to resultNetWeightCount
                        )
                    )
                    navigateUp()
                }
            }
        }
    }
}
