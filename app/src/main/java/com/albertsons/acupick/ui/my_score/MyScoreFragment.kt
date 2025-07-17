package com.albertsons.acupick.ui.my_score

import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.FragmentMyScoreBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MyScoreFragment : BaseFragment<MyScoreViewModel, FragmentMyScoreBinding>() {


    override val fragmentViewModel: MyScoreViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }
    override fun getLayoutRes() = R.layout.fragment_my_score

    override fun setupBinding(binding: FragmentMyScoreBinding) {
        super.setupBinding(binding)

        activityViewModel.setToolbarTitle(getString(R.string.toolbar_title_my_game))

        binding.lblHowToWin.setOnClickListener {
            val action = MyScoreFragmentDirections.actionMyScoreFragmentToFragmentHowToWin()
            findNavController().navigate(action)
        }
    }
}