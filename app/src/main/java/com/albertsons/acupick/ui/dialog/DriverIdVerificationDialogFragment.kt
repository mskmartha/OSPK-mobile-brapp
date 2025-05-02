package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.DriverIdVerificationDialogBinding

class DriverIdVerificationDialogFragment : BaseCustomDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<DriverIdVerificationDialogBinding>(inflater, R.layout.driver_id_verification_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
        }
    }
}
