package com.albertsons.acupick.ui.staging

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.UnassignTotesFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class UnAssignTotesFragment : BaseFragment<UnAssignTotesViewModel, UnassignTotesFragmentBinding>() {

    override val fragmentViewModel: UnAssignTotesViewModel by viewModel()

    private val args: UnAssignTotesFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.unassign_totes_fragment

    override fun setupBinding(binding: UnassignTotesFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner
        with(fragmentViewModel) {
            setParams(args.unAssignTotesParams)

            inputTotes.value = args.unAssignTotesParams.toteList
            activity.postValue(
                args.unAssignTotesParams
            )

            shortOrderId = args.unAssignTotesParams.shortOrderId
            customerName = args.unAssignTotesParams.customerName
            customerOrderNumber = args.unAssignTotesParams.customerOrderNumber!!
        }
    }
}
