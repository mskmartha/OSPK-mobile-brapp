package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.ui.missingItemLocation.WhereToFindLocationCodeScreen
import com.albertsons.acupick.ui.missingItemLocation.WhereToFindLocationViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class WhereToFindLocationCodeBottomSheet : BaseBottomSheetDialogFragment() {
    private val viewModel by viewModel<WhereToFindLocationViewModel>()
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(inflater, R.layout.base_compose_view_bottom_sheet, container, false).apply {
            baseComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WhereToFindLocationCodeScreen(viewModel)
                }
            }
            viewModel.navigation.observe(viewLifecycleOwner) {
                dismiss()
            }
        }
    }
}
