package com.albertsons.acupick.ui.bottomsheetdialog

import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.ViewModelItem
import com.xwray.groupie.viewbinding.BindableItem

class ActionSheetDbViewModel(
    val settings: ActionSheetOptions,
    private val settingsClickListener: (ActionSheetOptions) -> Unit
) : BaseBindableViewModel() {
    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> =
        { vm -> ViewModelItem(vm as ActionSheetDbViewModel, R.layout.item_action_sheet) }

    fun onSettingsClicked() {
        settingsClickListener(settings)
    }
}
