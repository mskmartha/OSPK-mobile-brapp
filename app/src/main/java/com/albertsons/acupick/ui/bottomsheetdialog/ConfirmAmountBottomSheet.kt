package com.albertsons.acupick.ui.bottomsheetdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.ConfirmAmountBottomSheetBinding
import com.albertsons.acupick.ui.models.ConfirmAmountUIData
import com.albertsons.acupick.ui.picklistitems.ConfirmAmountViewModel
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

/**  Confirm amount bottom sheet for PS type item */
class ConfirmAmountBottomSheet : BaseBottomSheetDialogFragment() {

    private val fragmentVm: ConfirmAmountViewModel by viewModel()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {

        return DataBindingUtil.inflate<ConfirmAmountBottomSheetBinding>(inflater, R.layout.confirm_amount_bottom_sheet, container, false).apply {
            val confirmAmountUIData = argData.customDataParcel as ConfirmAmountUIData
            lifecycleOwner = viewLifecycleOwner
            viewModel = fragmentVm
            uiData = confirmAmountUIData
            fragmentVm.itemType = confirmAmountUIData.itemType

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                fragmentVm.apply {
                    requestedAmount.postValue(confirmAmountUIData.requestedAmount)
                    resultNetWeightSharedFlow.collect { resultNetWeightCount ->
                        requireParentFragment().setFragmentResult(
                            CONFIRM_AMOUNT_REQUEST,
                            bundleOf(
                                CONFIRM_AMOUNT_REQUEST_RESULT to resultNetWeightCount.copy(itemType = confirmAmountUIData.itemType),
                            )
                        )
                        dismiss()
                    }
                }
            }
        }
    }
    companion object {
        const val CONFIRM_AMOUNT_REQUEST = "CONFIRM_AMOUNT_REQUEST"
        const val CONFIRM_AMOUNT_REQUEST_RESULT = "CONFIRM_AMOUNT_REQUEST_RESULT"
        const val CONFIRM_AMOUNT_REQUEST_ITEM_TYPE = "CONFIRM_AMOUNT_REQUEST_ITEM_TYPE"
    }
}
