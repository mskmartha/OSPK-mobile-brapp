package com.albertsons.acupick.ui.staging

import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.AddBagsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.dialog.CloseActionListenerProvider
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddBagsFragment : BaseFragment<AddBagsViewModel, AddBagsFragmentBinding>(), CloseActionListenerProvider {
    override val fragmentViewModel: AddBagsViewModel by viewModel()

    override fun getLayoutRes(): Int = R.layout.add_bags_fragment

    private val args: AddBagsFragmentArgs by navArgs()

    override fun setupBinding(binding: AddBagsFragmentBinding) {
        super.setupBinding(binding)
        binding.fragmentViewLifecycleOwner = viewLifecycleOwner

        fragmentViewModel.toteUiList.postValue(args.addBagsUiData.toteList.toMutableList())
        fragmentViewModel.isCustomerPreferBag.postValue(args.addBagsUiData.isCustomerPreferBag)
        fragmentViewModel.stagingActivityId.value = args.addBagsUiData.stagingId

        binding.shortOrderNumber.text = args.addBagsUiData.shortOrderId
        binding.headerOrderNumber.text = getString(R.string.number_format, args.addBagsUiData.customerOrderNumber)
        binding.customerName.text = args.addBagsUiData.customeName
    }
}
