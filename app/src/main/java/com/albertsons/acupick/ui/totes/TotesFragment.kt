package com.albertsons.acupick.ui.totes

import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.TotesFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.chat.ChatIconWithTooltip
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

/** Current pick list Info screen */
class TotesFragment : BaseFragment<TotesViewModel, TotesFragmentBinding>() {
    override val fragmentViewModel: TotesViewModel by sharedViewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    private val args: TotesFragmentArgs by navArgs()

    override fun getLayoutRes(): Int = R.layout.totes_fragment

    override fun setupBinding(binding: TotesFragmentBinding) {
        super.setupBinding(binding)
        binding.chatButtonView.setContent {
            ChatIconWithTooltip(onChatClicked = { orderNumber ->
                fragmentViewModel.onChatClicked(orderNumber)
            })
        }
        fragmentViewModel.pickListId = args.picklistid
        fragmentViewModel.toteEstimate = args.toteEstimate
        fragmentViewModel.unAssignSuccessfulAction.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_totesFragment_to_homeFragment)
        }
    }
}
