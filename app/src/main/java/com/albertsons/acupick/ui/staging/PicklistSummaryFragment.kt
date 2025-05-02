package com.albertsons.acupick.ui.staging

import androidx.core.view.marginStart
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemTotesBinding
import com.albertsons.acupick.ui.BaseFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Current pick list Info screen */
class PicklistSummaryFragment : BaseFragment<PicklistSummaryViewModel, ItemTotesBinding>() {
    override val fragmentViewModel: PicklistSummaryViewModel by viewModel()

    private val args: PicklistSummaryFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.item_totes

    override fun setupBinding(binding: ItemTotesBinding) {
        super.setupBinding(binding)
        with(fragmentViewModel) {
            pickListId = args.picklistid
            customerOrderNumber = args.customerordernumber
            isBlockingUi.observe(viewLifecycleOwner) {
                activityViewModel.setLoadingState(it, true)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                toteInfo.collect {
                    binding.apply {
                        totesUi = it
                        customerName.text = it.getCustomerName(customerName.marginStart)
                        isMfcOrder = it.activityDto?.isMultiSource
                        isVisible = true
                    }
                }
            }
            loadData(pickListId)
        }
    }
}
