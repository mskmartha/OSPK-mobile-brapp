package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.RadioButtonsDialogFragmentBinding
import com.albertsons.acupick.ui.util.StringIdHelper

class RadioButtonsDialogFragment : BaseCustomDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<RadioButtonsDialogFragmentBinding>(inflater, R.layout.radio_buttons_dialog_fragment, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            val radioButtonsStrings = argData.customData as? List<StringIdHelper>
            isWineShipping = argData.isWineOrder
            radioButtons = radioButtonsStrings?.map { it.getString(requireContext()) }
        }
    }
}
