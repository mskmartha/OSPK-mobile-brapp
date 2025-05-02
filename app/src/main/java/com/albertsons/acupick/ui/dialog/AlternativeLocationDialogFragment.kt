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
import com.albertsons.acupick.databinding.AlternativeLocationsDialogBinding
import com.albertsons.acupick.ui.models.AlternativeLocationItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AlternativeLocationDialogFragment : BaseCustomDialogFragment() {
    private val fragmentVm: AlternativeLocationViewModel by viewModel()

    override val shouldFillScreen
        get() = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<AlternativeLocationsDialogBinding>(inflater, R.layout.alternative_locations_dialog, container, false).apply {

            val altLocData = argData.customData as AlternativeLocationItem
            viewData = argData.toViewData(requireContext())
            viewModel = fragmentVm
            item = altLocData
            fragmentVm.path = altLocData.path.ordinal

            fragmentVm.navigation.observe(viewLifecycleOwner) { closeAction ->
                Timber.v("[setupBinding closeActionEvent] closeAction=$closeAction")
                dismiss()
                // Need to invoke this close action *after* the dialog has been dismissed to allow another dialog to be shown from the close action if desired.
                findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
            }
        }
    }
}
