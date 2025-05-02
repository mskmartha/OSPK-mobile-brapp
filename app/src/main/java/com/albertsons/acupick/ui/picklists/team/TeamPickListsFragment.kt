package com.albertsons.acupick.ui.picklists.team

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PickListsFragmentBinding
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.picklists.PickListsBaseFragment
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TeamPickListsFragment : PickListsBaseFragment<TeamPickListsViewModel, PickListsFragmentBinding>() {
    override val fragmentViewModel: TeamPickListsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes(): Int = R.layout.pick_lists_fragment
}
