package com.albertsons.acupick.ui.arrivals.complete

import android.content.Context
import android.util.DisplayMetrics
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isBeginVerificationState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isItemsRemovedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRxRemoveRestrictedItemsState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifiedPickupPersonState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyCodeState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyingCodeState
import com.albertsons.acupick.data.logic.HandOffVerificationState
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode
import com.albertsons.acupick.ui.BaseBindableViewModel
import com.albertsons.acupick.ui.LiveDataHelper
import com.albertsons.acupick.ui.ViewModelItem
import com.albertsons.acupick.ui.util.orFalse
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class HandOffDbViewModel(
    val item: HandOffRegulatedItem,
) : LiveDataHelper, BaseBindableViewModel() {

    var count = item.totalQty.toInt().toString()
    var description = item.description

    override fun getItemFactory(): (BaseBindableViewModel) -> BindableItem<ViewDataBinding> {
        return { vm -> ViewModelItem(vm as HandOffDbViewModel, R.layout.hand_off_recycler_item) }
    }
}

@BindingAdapter(value = ["app:handOffItems", "app:handOffViewModel", "app:showRvItems"])
fun RecyclerView.setHandOffList(items: List<HandOffRegulatedItem>?, viewModel: HandOffViewModel?, showRvItems: Boolean = false) {
    if (items == null || viewModel == null) return

    @Suppress("UNCHECKED_CAST")
    (adapter as? GroupAdapter<GroupieViewHolder>)?.apply {
        // Update adapter with new info.
        clear()
        addContents(items, viewModel, showRvItems)
    } ?: run {
        layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter<GroupieViewHolder>().apply {
            addContents(items, viewModel, showRvItems)
        }
    }
}

private fun GroupAdapter<GroupieViewHolder>.addContents(items: List<HandOffRegulatedItem>, viewModel: HandOffViewModel, showItems: Boolean) {
    if (showItems) {
        add(generateHandOffItems(items))
    }
}

private fun generateHandOffItems(items: List<HandOffRegulatedItem>): Section = Section().apply { update(items.map { HandOffDbViewModel(item = it) }) }

@BindingAdapter(value = ["app:onRvVisible", "app:scrollSpeed"])
fun RecyclerView.onRvVisible(itemsShown: Boolean?, scrollSpeed: Float) {
    if (itemsShown == true) {
        val linearSmoothScroller: LinearSmoothScroller = object : LinearSmoothScroller(this@onRvVisible.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return scrollSpeed / this@onRvVisible.computeVerticalScrollRange()
            }
        }
        linearSmoothScroller.targetPosition = this.bottom
        layoutManager?.startSmoothScroll(linearSmoothScroller)
    }
}

@BindingAdapter(value = ["app:customerIndex", "app:customerCount", "handoffUiState", "is3p"], requireAll = false)
fun AppCompatTextView.setCustomerCompleteButtonText(customerIndex: Int, customerCount: Int, handoffUiState: HandOffVerificationState?, is3p: Boolean?) =

    when {
        handoffUiState.isBeginVerificationState() -> setText(context.getString(R.string.begin_age_verification))
        is3p.orFalse() -> setText(context.getString(R.string.complete_handoff))
        handoffUiState.isVerifyCodeState() ||
            handoffUiState.isVerifyingCodeState() -> setText(context.getString(R.string.verify_code))
        handoffUiState.isItemsRemovedState() -> setText(context.getString(R.string.confirm_handoff))
        handoffUiState.isItemsRemovedState() ||
            handoffUiState.isVerifiedPickupPersonState() -> setText(context.getString(R.string.confirm_handoff))
        customerCount == 1 -> setText(context.getString(R.string.mark_as_complete))
        else -> setText(context.getString(R.string.mark_as_complete_x_of_y_format, customerIndex, customerCount))
    }

@BindingAdapter(value = ["app:rxDugEnabled", "app:hasDugRegulatedItems", "handoffUiState"], requireAll = false)
fun AppCompatTextView.setRxDugButtonText(rxDugEnabled: Boolean, hasDugRegulatedItems: Boolean, handoffUiState: HandOffVerificationState?) {
    this.text = if (rxDugEnabled && hasDugRegulatedItems) {
        if (handoffUiState.isRxRemoveRestrictedItemsState())
            this.context.getString(R.string.rx_items_removed_cta)
        else
            this.context.getString(R.string.begin_age_verification)
    } else if (rxDugEnabled && handoffUiState.isRxRemoveRestrictedItemsState()) {
        this.context.getString(R.string.rx_items_removed_cta)
    } else {
        this.context.getString(R.string.complete_handoff)
    }
}

@BindingAdapter("app:setTextOrNotAvailable")
fun AppCompatTextView.setTextOrNotAvailable(handOffUiParam: String?) {
    this.text = if (handOffUiParam?.isNullOrEmpty() == true) this.context.getString(R.string.info_not_available) else handOffUiParam
}

// if the order status was READY_FOR_PICKUP, at the time of destaging then we show the Rx related failure text for auth failures
@BindingAdapter(value = ["app:isAuthCodeVerified", "app:authCodeIssueReported", "app:authCodefailureReasonCode", "app:isRxDug", "app:rxOrderStatus"], requireAll = false)
fun AppCompatTextView.setVerifiedText(
    isAuthCodeVerified: Boolean?,
    authCodeIssueReported: Boolean?,
    authCodefailureReasonCode: AuthCodeUnavailableReasonCode?,
    isRxDug: Boolean,
    isRxDeliveryReadyForPU: Boolean,
) {
    text = when {
        isAuthCodeVerified == true -> context.getString(R.string.order_verified)
        authCodeIssueReported == true && authCodefailureReasonCode != null -> getAuthCodeFailureReason(authCodefailureReasonCode, isRxDug && isRxDeliveryReadyForPU, context)
        authCodeIssueReported == true -> context.getString(R.string.order_report_logged_unavailable_code)
        else -> ""
    }
}

private fun getAuthCodeFailureReason(authCodefailureReasonCode: AuthCodeUnavailableReasonCode, isRxDugReadyForPU: Boolean, context: Context): String {
    return when (authCodefailureReasonCode) {
        AuthCodeUnavailableReasonCode.NO_CUSTOMER_CODE, AuthCodeUnavailableReasonCode.PICKED_UP_BY_SOMEONE_ELSE -> {
            if (isRxDugReadyForPU) context.getString(R.string.order_report_logged_unavailable_code_rx_dug) else
                context.getString(R.string.order_report_logged_unavailable_code)
        }
        // AuthCodeUnavailableReasonCode.PICKED_UP_BY_SOMEONE_ELSE -> context.getString(R.string.order_report_logged_unavailable_code)
        AuthCodeUnavailableReasonCode.WRONG_CUSTOMER_CODE -> {
            if (isRxDugReadyForPU) context.getString(R.string.order_report_logged_invalid_code_rx_dug) else
                context.getString(R.string.order_report_logged_invalid_code)
        }
        AuthCodeUnavailableReasonCode.NO_AUTHENTICATION_CODE_PROVIDED -> context.getString(R.string.order_report_logged_invalid_code)
        else -> ""
    }
}
