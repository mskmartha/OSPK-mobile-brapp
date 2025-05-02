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
import com.albertsons.acupick.databinding.Modal5ConfirmationDialogBinding

class Modal5ConfirmationDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<Modal5ConfirmationDialogBinding>(inflater, R.layout.modal5_confirmation_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            showButtonIcon = argData.customData as? Boolean ?: false // TODO: Need to verify the usage
            customerArrivalTime = argData.cutomerArrivalTime
        }
}
