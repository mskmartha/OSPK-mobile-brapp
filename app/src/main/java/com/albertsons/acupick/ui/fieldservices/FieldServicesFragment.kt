package com.albertsons.acupick.ui.fieldservices

import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.FieldServicesFragmentBinding
import com.albertsons.acupick.ui.BaseFragment
import com.albertsons.acupick.ui.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.getSharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Represents the screen used by Field Services Techs + ALB QA to test environment connectivity as well as image display capabilities, including overriding environments used by the app.
 *
 * [Zeplin](https://app.zeplin.io/project/5fc96b427a23e061060735d6/dashboard)
 */
class FieldServicesFragment : BaseFragment<FieldServicesViewModel, FieldServicesFragmentBinding>() {
    override val fragmentViewModel: FieldServicesViewModel by viewModel {
        parametersOf(getSharedViewModel<MainActivityViewModel>())
    }

    override fun getLayoutRes(): Int = R.layout.field_services_fragment

    override fun setupBinding(binding: FieldServicesFragmentBinding) {
        super.setupBinding(binding)
        activityViewModel.setToolbarTitle(getString(R.string.toolbar_title_field_services_test_connectivity))
        activityViewModel.setToolbarRightExtraCta(getString(R.string.reset)) { fragmentViewModel.onResetCtaClicked() }
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentViewModel.saveBaseUrls()
    }
}
