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
import com.albertsons.acupick.databinding.CustomRadioButtonsDialogFragmentBinding
import com.albertsons.acupick.ui.util.StringIdHelper

class CustomRadioButtonsDialogFragment : BaseCustomDialogFragment() {
    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<CustomRadioButtonsDialogFragmentBinding>(inflater, R.layout.custom_radio_buttons_dialog_fragment, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            val radioButtonsStrings = argData.customData as? List<StringIdHelper>
            isWineShipping = argData.isWineOrder
            radioButtons = radioButtonsStrings?.map { it.getString(requireContext()) }
        }
    }
}
