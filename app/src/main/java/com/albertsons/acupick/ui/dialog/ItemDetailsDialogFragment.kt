package com.albertsons.acupick.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ItemDetailDialogFragmentBinding

class ItemDetailsDialogFragment : BaseCustomDialogFragment() {

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<ItemDetailDialogFragmentBinding>(inflater, R.layout.item_detail_dialog_fragment, container, false).apply {
            viewData = argData.toViewData(requireContext())
        }
    }
}
