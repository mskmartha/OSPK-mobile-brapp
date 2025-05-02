package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.PharmacyStaffRequiredDialogBinding

class PharmacyStaffRequiredDialogFragment : BaseCustomDialogFragment() {
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<PharmacyStaffRequiredDialogBinding>(inflater, R.layout.pharmacy_staff_required_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
        }
}
