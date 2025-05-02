package com.albertsons.acupick.ui.staging.print

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PrintLabelsFragmentBinding
import com.albertsons.acupick.ui.BaseFragment

class PrintLabelsFragment : BaseFragment<PrintLabelsViewModel, PrintLabelsFragmentBinding>() {
    override val fragmentViewModel: PrintLabelsViewModel by viewModels()
    override fun getLayoutRes(): Int = R.layout.print_labels_fragment

    // Incoming arguments
    private val args: PrintLabelsFragmentArgs by navArgs()

    override fun setupBinding(binding: PrintLabelsFragmentBinding) {
        super.setupBinding(binding)

        binding.customLifecycleOwner = viewLifecycleOwner
        fragmentViewModel.setupLabelList(args.printLabelUi.printLabelsUi, args.isCustomerPreferBag)
        fragmentViewModel.activityId.postValue(args.activityId)

        binding.customerName.text = args.printLabelUi.printLabelsUi?.getOrNull(0)?.customeName
        binding.shortOrderNumber.text = args.printLabelUi.printLabelsUi?.getOrNull(0)?.shortOrderId
        binding.headerOrderNumber.text = getString(R.string.number_format, args.printLabelUi.printLabelsUi?.getOrNull(0)?.customerOrderNumber)

        fragmentViewModel.printButtonEnabled.observe(viewLifecycleOwner) {
            binding.printLabelButton.isEnabled = it
        }
    }
}
