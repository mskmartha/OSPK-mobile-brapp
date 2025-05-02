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
import com.albertsons.acupick.data.model.response.SwapItemParams
import com.albertsons.acupick.databinding.RemoveSubstitutionDialogBinding

class RemoveSubstitutionDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<RemoveSubstitutionDialogBinding>(inflater, R.layout.remove_substitution_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            swapItem = argData.customData as SwapItemParams
        }
}
