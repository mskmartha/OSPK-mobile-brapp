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
import com.albertsons.acupick.databinding.EbtNobagsWarningDialogBinding
import com.albertsons.acupick.databinding.WarningDialogBinding

class WarningDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        val style = argData.dialogType
        return if (style == DialogType.EbtWarning) {
            DataBindingUtil.inflate<WarningDialogBinding>(inflater, R.layout.warning_dialog, container, false).apply {
                viewModel = fragmentViewModel
                viewData = argData.toViewData(requireContext())
            }
        } else {
            DataBindingUtil.inflate<EbtNobagsWarningDialogBinding>(inflater, R.layout.ebt_nobags_warning_dialog, container, false).apply {
                viewModel = fragmentViewModel
                viewData = argData.toViewData(requireContext())
            }
        }
    }
}
