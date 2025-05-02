package com.albertsons.acupick.ui.dialog

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding

class OnePlGiftingDialogFragment : BaseCustomDialogFragment() {

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(inflater, R.layout.base_compose_view_bottom_sheet, container, false).apply {
            baseComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    OnePlGiftingDialogScreen(
                        viewModel = fragmentViewModel,
                        title = argData.title.getString(requireContext()),
                        largeImage = argData.largeImage,
                        body = argData.body?.getString(requireContext()),
                        secondaryBody = argData.secondaryBody?.getString(requireContext()),
                        boldWord = argData.boldWord?.getString(requireContext()),
                        positiveButtonText = argData.positiveButtonText?.getString(requireContext())
                    )
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCancelable(false)
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
        }
    }
}
