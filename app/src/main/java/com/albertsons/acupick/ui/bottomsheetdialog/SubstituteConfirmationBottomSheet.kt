package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.databinding.SubstituteConfirmationBottomSheetBinding
import com.albertsons.acupick.ui.dialog.CloseAction
import com.albertsons.acupick.ui.dialog.findDialogListener
import com.albertsons.acupick.ui.substitute.SubstituteConfirmationParam
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.REMOVE_SUBSTITUTION_REQUEST_KEY
import com.albertsons.acupick.ui.substitute.SubstituteViewModel.Companion.REMOVE_SUBSTITUTION_RESULTS
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

const val MAX_SWIPE_DOWN_OFFSET_LIMIT = -0.5

/** Bottom sheet for confirmation of substituted item */
class SubstituteConfirmationBottomSheet : BaseBottomSheetDialogFragment() {
    private val substituteConfirmationViewModel: SubstituteConfirmationViewModel by viewModel()
    private lateinit var standardBottomSheetBehavior: BottomSheetBehavior<View>
    private var isSwipeDown: Boolean = true
    override fun getViewDataBinding(inflater: LayoutInflater, container: ViewGroup?): ViewDataBinding {
        return DataBindingUtil.inflate<SubstituteConfirmationBottomSheetBinding>(inflater, R.layout.substitute_confirmation_bottom_sheet, container, false).apply {
            viewData = argData.toViewData(requireContext())
            val substituteData = argData.customDataParcel as SubstituteConfirmationParam
            lifecycleOwner = viewLifecycleOwner
            viewModel = substituteConfirmationViewModel
            sharedViewModel.isLoading.observe(viewLifecycleOwner) {
                isLoading = it
            }
            substituteConfirmationViewModel.apply {
                isIssueScanning.value = substituteData.hadIssueScanning
                sellByType.value = substituteData.sellByType
                isRequestedWeightToShow.value = substituteData.hadIssueScanning && sellByType.value != SellByType.RegularItem && sellByType.value != SellByType.PriceEach &&
                    sellByType.value != SellByType.Each && !substituteData.isDisplayType3PW
                subListItemUi.value = substituteData.substituteItemList
                imageUrl.value = substituteData.imageUrl
                description.value = substituteData.description
                requestedCount.value = substituteData.requestedCount
                isOrderedByWeight.value = substituteData.isOrderedByWeight
                isDisplayType3PW.value = substituteData.isDisplayType3PW
                requestedWeightAndUnits.value = substituteData.requestedWeightAndUnits
                isBulk.value = substituteData.isBulk
                iaId.value = substituteData.iaId
                siteId.value = substituteData.siteId
                messageSid.value = substituteData.messageSid
                isCustomerBagPreference.value = substituteData.isCustomerBagPreference
                // TODO: ACURED_REDESIGN Will use different live data for substitute confirmation bottomsheet to observe live events
                sharedViewModel.bottomSheetRecordPickArgData.observe(viewLifecycleOwner) {
                    if (it.exit && it.dialogType == BottomSheetType.SubstitutionConfirmation) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            isSwipeDown = false // Once all substituted item removed the bottomsheet will slide down without pop the warning dialog
                            delay(500) //
                            dismissBottomSheet()
                            findDialogListener()?.onCloseAction(CloseAction.Dismiss, null)
                        }
                    }
                    it.customDataParcel?.let { bottomSheetArg ->
                        invalidateAll() // TODO: ACURED_REDESIGN View not updated so invalidateAll() called explicitly, need to find suitable solution here
                        subListItemUi.value = (bottomSheetArg as SubstituteConfirmationParam).substituteItemList
                    }
                }

                // TODO: ACURED_REDESIGN Refactor it with live data event instead of DialogListener on CloseAction
                navigation.observe(viewLifecycleOwner) { closeAction ->
                    Timber.v("[Substitute confirmation bottomsheet setupBinding closeActionEvent] closeAction=$closeAction")
                    if (closeAction.first == CloseAction.Positive) {
                        isSwipeDown = false // Once substitution process completed the bottomsheet will slide down without pop the warning dialog
                        dismissBottomSheet()
                    }
                    // No need to dismiss bottom sheet on CloseAction.Negative
                    findDialogListener()?.onCloseAction(closeAction.first, closeAction.second)
                }
                itemToRemove.observe(viewLifecycleOwner) {
                    requireParentFragment().setFragmentResult(
                        REMOVE_SUBSTITUTION_REQUEST_KEY,
                        bundleOf(REMOVE_SUBSTITUTION_RESULTS to it)
                    )
                }
                /**
                 * ACIP-142811 This is used to observe loading state of recordItemPickComplete API call from bottomsheet viewmodel
                 * when complete button get called. It will not allow the picker to click it again until API provides any response.
                 */
                isBlockingUi.observe(viewLifecycleOwner) {
                    isLoading = it
                }
            }
        }
    }

    private fun resetBottomsheetAttributes() {
        isSwipeDown = true
        standardBottomSheetBehavior.isHideable = true
        standardBottomSheetBehavior.isDraggable = true
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
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            Timber.v("BottomSheetBehavior STATE_COLLAPSED")
                            if (!isSwipeDown) {
                                sharedViewModel.bottomSheetBackButtonPressed.postValue(Unit)
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        if (slideOffset < MAX_SWIPE_DOWN_OFFSET_LIMIT && isSwipeDown) {
                            Timber.v("BottomSheetBehavior slide offset $slideOffset")
                            isSwipeDown = false
                            standardBottomSheetBehavior.isDraggable = false
                            standardBottomSheetBehavior.isHideable = false
                            standardBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(2000)
                                resetBottomsheetAttributes()
                            }
                        }
                    }
                })
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { _: DialogInterface, keyCode: Int, keyEvent: KeyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                    sharedViewModel.bottomSheetBackButtonPressed.postValue(Unit)
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
    }
}
