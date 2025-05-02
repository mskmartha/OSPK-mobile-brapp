package com.albertsons.acupick.ui.arrivals.complete

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.HANDOFF_REMOVED_ITEMS
import com.albertsons.acupick.ui.arrivals.complete.HandOffViewModel.Companion.HANDOFF_REMOVE_ITEMS_REQUEST_KEY
import com.albertsons.acupick.ui.bottomsheetdialog.BaseBottomSheetDialogFragment
import com.albertsons.acupick.ui.bottomsheetdialog.BottomSheetType
import org.koin.androidx.viewmodel.ext.android.viewModel

class HandOffRemoveItemsBottomSheet : BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<HandOffRemoveItemsViewModel>()

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(inflater, R.layout.base_compose_view_bottom_sheet, container, false).apply {
            val param = argData.customDataParcel as? HandOffRemovalParams
            param?.let {
                viewModel.setItems(
                    it.isRx,
                    if (it.isRx) {
                        it.handOffUI?.rxOrderIds?.map { rxId -> RestrictedItem(description = rxId, isChecked = false) } ?: emptyList()
                    } else {
                        it.handOffUI?.items?.map { item -> item.toRestrictedItem() } ?: emptyList()
                    }
                )
                baseComposeView.apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        HandOffRemoveItemsScreen(viewModel = viewModel)
                    }
                }
                viewModel.apply {
                    collectFlow(removeItemsConfirmedEvent) {
                        setFragmentResult(
                            HANDOFF_REMOVE_ITEMS_REQUEST_KEY,
                            bundleOf(
                                HANDOFF_REMOVED_ITEMS to true
                            )
                        )
                    }

                    collectFlow(removeItemsCancelledEvent) {
                        dismiss()
                    }

                    sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                        if (it.exit && it.dialogType == BottomSheetType.HandOffRemoveItems) {
                            dismiss()
                        }
                    }
                }
            } ?: run {
                // Handle the case where param is null
                dismiss()
            }
        }
    }
}
