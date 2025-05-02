package com.albertsons.acupick.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.SubstituteItemAlertDialogBinding

class SubbedItemAlertDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<SubstituteItemAlertDialogBinding>(inflater, R.layout.substitute_item_alert_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
        }
    }
}
