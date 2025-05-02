package com.albertsons.acupick.ui.bottomsheetdialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import com.albertsons.acupick.R
import com.albertsons.acupick.databinding.BaseComposeViewBottomSheetBinding
import com.albertsons.acupick.infrastructure.utils.collectFlow
import com.albertsons.acupick.infrastructure.utils.emitToFlow
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationScreen
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationViewModel
import com.albertsons.acupick.ui.missingItemLocation.MissingItemLocationViewModel.Companion.WHERE_TO_FIND_LOCATION_CODE_REQUEST_KEY
import com.albertsons.acupick.ui.models.MissingItemLocationParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MissingItemLocationBottomSheet : BaseBottomSheetDialogFragment() {

    private val viewModel by viewModel<MissingItemLocationViewModel>()

    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<View>
    // To avoid multiple dismiss calls
    private var isDismissTriggered: Boolean = false

    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<BaseComposeViewBottomSheetBinding>(inflater, R.layout.base_compose_view_bottom_sheet, container, false).apply {
            val param = argData.customDataParcel as MissingItemLocationParams
            baseComposeView.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MissingItemLocationScreen(viewModel, param)
                }
            }
            viewModel.apply {
                collectFlow(addLocationEvent) {
                    requireParentFragment().setFragmentResult(
                        MissingItemLocationViewModel.MISSING_ITEM_LOCATION_REQUEST_KEY,
                        bundleOf(
                            MissingItemLocationViewModel.MISSING_ITEM_LOCATION_RESULTS to it,
                            MissingItemLocationViewModel.DATA_PICKSLIST_SCANNED to param.scannedData
                        )
                    )
                }

                collectFlow(whereToFindLocationEvent) {
                    parentFragment?.setFragmentResult(
                        WHERE_TO_FIND_LOCATION_CODE_REQUEST_KEY,
                        bundleOf()
                    )
                }
                collectFlow(notNowEvent) {
                    if (!isDismissTriggered) {
                        isDismissTriggered = true
                        popUp(param)
                        if (dialog?.isShowing == true && standardBottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss()
                        }
                    }
                }
            }
            sharedViewModel.apply {
                bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                    if (it.exit) {
                        isDismissTriggered = true
                        dismiss()
                    } else {
                        emitToFlow(viewModel.isErrorShown, true)
                    }
                }
                isLoading.observe(viewLifecycleOwner) {
                    it?.let { emitToFlow(viewModel.isLoading, it) }
                }
            }
        }
    }

    private fun popUp(param: MissingItemLocationParams) {
        requireParentFragment().setFragmentResult(
            MissingItemLocationViewModel.MISSING_ITEM_LOCATION_NOT_NOW_REQUEST_KEY,
            bundleOf(
                MissingItemLocationViewModel.DATA_PICKSLIST_SCANNED to param.scannedData
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog
            bottomSheetDialog?.dismissWithAnimation = true
            val bottomSheet = bottomSheetDialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                standardBottomSheetBehavior = BottomSheetBehavior.from(it)
                it.layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                standardBottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(argData.peekHeight)
                standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                standardBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            Timber.v("BottomSheetBehavior STATE_COLLAPSED")
                            if (!isDismissTriggered) {
                                viewModel.onClickNotNow()
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
    }
}
