package com.albertsons.acupick.ui.picklists.open

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PickListsFragmentBinding
import com.albertsons.acupick.ui.MainActivityViewModel
import com.albertsons.acupick.ui.picklists.PickListsBaseFragment
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/** Open/Available Pick Lists selection screen ([Zeplin](https://zpl.io/bodMZ5G)) */
class OpenPickListsFragment : PickListsBaseFragment<OpenPickListsViewModel, PickListsFragmentBinding>() {
    override val fragmentViewModel: OpenPickListsViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes(): Int = R.layout.pick_lists_fragment
}
