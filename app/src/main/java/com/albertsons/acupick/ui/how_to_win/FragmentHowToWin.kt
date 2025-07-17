package com.albertsons.acupick.ui.how_to_win

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.FragmentHowToWinBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FragmentHowToWin : BaseFragment<HowToWinViewModel, FragmentHowToWinBinding>() {

    override val fragmentViewModel: HowToWinViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }
    override fun getLayoutRes() = R.layout.fragment_how_to_win

    override fun setupBinding(binding: FragmentHowToWinBinding) {
        super.setupBinding(binding)

        activityViewModel.setToolbarTitle(getString(R.string.toolbar_title_my_game))
    }
}