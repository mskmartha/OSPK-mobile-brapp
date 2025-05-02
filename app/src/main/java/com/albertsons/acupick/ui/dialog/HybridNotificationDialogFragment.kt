package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.HybridNotificationDialogBinding

class HybridNotificationDialogFragment : BaseCustomDialogFragment() {
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding =
        DataBindingUtil.inflate<HybridNotificationDialogBinding>(inflater, R.layout.hybrid_notification_dialog, container, false).apply {
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentViewModel
            showButtonIcon = argData.customData as? Boolean ?: false
        }
}
