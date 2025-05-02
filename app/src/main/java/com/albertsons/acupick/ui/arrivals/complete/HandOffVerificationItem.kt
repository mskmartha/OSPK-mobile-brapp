package com.albertsons.acupick.ui.arrivals.complete

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.albertsons.acupick.R
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isBeginVerificationState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isCodeVerifiedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isItemsRemovedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRemoveRestrictedItemsState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRxItemsRemovedState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isRxRemoveRestrictedItemsState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifiedPickupPersonState
import com.albertsons.acupick.data.logic.AgeVerificationLogic.isVerifyingCodeState
import com.albertsons.acupick.data.logic.HandOffVerificationState
import com.albertsons.acupick.databinding.HandoffVerificationCodeBinding
import com.albertsons.acupick.databinding.ItemHandoffConfirmOrderBinding
import com.albertsons.acupick.databinding.ItemHandoffHeaderBinding
import com.albertsons.acupick.databinding.ItemHandoffIdVerificationBinding
import com.albertsons.acupick.databinding.ItemHandoffIdVerifiedBinding
import com.albertsons.acupick.databinding.ItemHandoffOrderInfoBinding
import com.albertsons.acupick.databinding.ItemHandoffOrderVerifiedBinding
import com.albertsons.acupick.databinding.ItemHandoffRemoveItemsBinding
import com.albertsons.acupick.databinding.ItemHandoffVerificationCodeBinding
import com.albertsons.acupick.databinding.ItemLegacyHandoffIdVerificationBinding
import com.albertsons.acupick.databinding.ItemRestrictedItemsRemovedBinding
import com.albertsons.acupick.ui.bindingadapters.setInvalidIdBySelected
import com.albertsons.acupick.ui.bindingadapters.setValidIdBySelected
import com.albertsons.acupick.ui.util.SmoothScrollLayoutManager
import com.albertsons.acupick.ui.util.groupieAdapter
import com.albertsons.acupick.ui.util.orFalse
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem

class HandOffVerificationHeader(
    private val viewModel: HandOffViewModel,
) : BindableItem<ItemHandoffHeaderBinding>() {
    override fun initializeViewBinding(view: View) = ItemHandoffHeaderBinding.bind(view)

    override fun getLayout() = R.layout.item_handoff_header
    override fun bind(viewBinding: ItemHandoffHeaderBinding, position: Int) {
        viewBinding.viewModel = viewModel
    }
}

class LegacyHandOffVerificationItem(
    private val handoffViewModel: HandOffViewModel,
) : BindableItem<ItemLegacyHandoffIdVerificationBinding>() {
    override fun initializeViewBinding(view: View) = ItemLegacyHandoffIdVerificationBinding.bind(view)
    override fun getLayout() = R.layout.item_legacy_handoff_id_verification
    override fun bind(viewBinding: ItemLegacyHandoffIdVerificationBinding, position: Int) {
        viewBinding.apply {
            viewModel = handoffViewModel
            idValidImage.setOnClickListener {
                handoffViewModel.onValidIdClicked()
                idValidImage.setValidIdBySelected(true)
                idInValidImage.setInvalidIdBySelected(false)
                handOffRecyclerView.onRvVisible(false, 1000f)
                handOffRecyclerView.setHandOffList(viewModel?.handOffUI?.value?.items, handoffViewModel, false)
                handOffRemoveItemsLabel.visibility = View.GONE
            }
            idInValidImage.setOnClickListener {
                handoffViewModel.onInvalidIdClicked()
                idValidImage.setValidIdBySelected(false)
                idInValidImage.setInvalidIdBySelected(true)
                handOffRemoveItemsLabel.visibility = View.VISIBLE
                handOffRecyclerView.onRvVisible(true, 1000f)
                handOffRecyclerView.setHandOffList(viewModel?.handOffUI?.value?.items, handoffViewModel, true)
            }
        }
    }
}

class HandOffIdVerificationItem(
    private val handoffViewModel: HandOffViewModel,
) : BindableItem<ItemHandoffIdVerificationBinding>() {
    override fun initializeViewBinding(view: View) = ItemHandoffIdVerificationBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_id_verification
    override fun bind(viewBinding: ItemHandoffIdVerificationBinding, position: Int) {
        viewBinding.apply {
            viewModel = handoffViewModel
        }
    }
}

class HandOffIdVerifiedItem(
    private val handoffViewModel: HandOffViewModel,
) : BindableItem<ItemHandoffIdVerifiedBinding>() {
    override fun initializeViewBinding(view: View) = ItemHandoffIdVerifiedBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_id_verified
    override fun bind(viewBinding: ItemHandoffIdVerifiedBinding, position: Int) {
        viewBinding.apply {
            viewModel = handoffViewModel
        }
    }
}

class HandOffVerificationOrderInfo(
    private val handOffUI: HandOffUI,
    private val viewModel: HandOffViewModel,
    private val fragmentLifecycleOwner: LifecycleOwner? = null,
) : BindableItem<ItemHandoffOrderInfoBinding>() {

    override fun initializeViewBinding(view: View) = ItemHandoffOrderInfoBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_order_info
    override fun bind(viewBinding: ItemHandoffOrderInfoBinding, position: Int) {
        viewBinding.handOffUI = handOffUI
        viewBinding.viewModel = viewModel
        viewBinding.lifecycleOwner = fragmentLifecycleOwner
    }
}

class HandOffVerificationOrderVerifiedItem(
    private val viewModel: HandOffViewModel,
    private val fragmentLifecycleOwner: LifecycleOwner? = null,
) : BindableItem<ItemHandoffOrderVerifiedBinding>() {
    override fun initializeViewBinding(view: View) = ItemHandoffOrderVerifiedBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_order_verified
    override fun bind(viewBinding: ItemHandoffOrderVerifiedBinding, position: Int) {
        viewBinding.viewModel = viewModel
        viewBinding.lifecycleOwner = fragmentLifecycleOwner
    }
}

class HandOffVerificationCode(
    private val handOffUI: HandOffUI,
    private val handoffViewModel: HandOffViewModel,
) : BindableItem<ItemHandoffVerificationCodeBinding>() {

    override fun initializeViewBinding(view: View) = ItemHandoffVerificationCodeBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_verification_code
    override fun bind(viewBinding: ItemHandoffVerificationCodeBinding, position: Int) {

        viewBinding.apply {
            viewModel = handoffViewModel

            authenticatedDugLayout.handleAuthDugInputVisibility(
                handOffUI.isAuthDugEnabled.orFalse(),
                handoffViewModel.codeVerifiedOrReportLogged.value.orFalse()
            )
        }
    }
}

class HandOffVerificationCodeLoadingItem : BindableItem<HandoffVerificationCodeBinding>() {
    var binding: HandoffVerificationCodeBinding? = null
    override fun initializeViewBinding(view: View) = HandoffVerificationCodeBinding.bind(view)
    override fun getLayout() = R.layout.handoff_verification_code
    override fun bind(viewBinding: HandoffVerificationCodeBinding, position: Int) {}
}

class HandOffVerificationItemsRemovedItem(
    private val viewModel: HandOffViewModel
) : BindableItem<ItemRestrictedItemsRemovedBinding>() {
    override fun initializeViewBinding(view: View) = ItemRestrictedItemsRemovedBinding.bind(view)
    override fun getLayout() = R.layout.item_restricted_items_removed
    override fun bind(viewBinding: ItemRestrictedItemsRemovedBinding, position: Int) {
        viewBinding.viewModel = viewModel
    }
}

class HandOffVerificationConfirmOrderItem(
    private val handOffUI: HandOffUI,
    private val viewModel: HandOffViewModel,
) : BindableItem<ItemHandoffConfirmOrderBinding>() {
    override fun initializeViewBinding(view: View) = ItemHandoffConfirmOrderBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_confirm_order
    override fun bind(viewBinding: ItemHandoffConfirmOrderBinding, position: Int) {
        viewBinding.handOffUI = handOffUI
        viewBinding.viewModel = viewModel
    }
}

class HandOffRemoveItems(
    private val fragmentLifecycleOwner: LifecycleOwner?,
    private val viewModel: HandOffViewModel,
    private val handOffUI: HandOffUI,
    private val uiState: HandOffVerificationState?,
) : BindableItem<ItemHandoffRemoveItemsBinding>() {
    var binding: ItemHandoffRemoveItemsBinding? = null
    override fun initializeViewBinding(view: View) = ItemHandoffRemoveItemsBinding.bind(view)
    override fun getLayout() = R.layout.item_handoff_remove_items
    override fun bind(viewBinding: ItemHandoffRemoveItemsBinding, position: Int) {
        viewModel.removeItemsCtaEnabled.postValue(true)
        viewBinding.viewModel = viewModel
        viewBinding.handOffUI = handOffUI
        viewBinding.fragmentLifecycleOwner = fragmentLifecycleOwner
        // TODO will be removed once the redesign is completed
        // viewBinding.regulatedItems.setRegulatedItems(handOffUI, viewModel, uiState, fragmentLifecycleOwner)
    }
}

@BindingAdapter(value = ["app:handOffUI", "app:viewModel", "app:handOffVerificationState", "app:fragmentLifecycleOwner"], requireAll = false)
fun RecyclerView.handoffUI(
    handOffUI: HandOffUI?,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
    fragmentLifecycleOwner: LifecycleOwner?,
) {
    if (handOffUI == null) return

    layoutManager = SmoothScrollLayoutManager(context)
    adapter = GroupieAdapter().apply { generateGroup(fragmentLifecycleOwner, handOffUI, viewModel, uiState) }
    if (uiState.isVerifyingCodeState() || uiState.isVerifiedPickupPersonState() || uiState.isBeginVerificationState() ||
        uiState.isRemoveRestrictedItemsState() || uiState.isItemsRemovedState() || uiState.isRxItemsRemovedState()
    ) {
        this.smoothScrollToPosition(this.groupieAdapter?.itemCount ?: 0)
    }
}

private fun GroupieAdapter.generateGroup(
    fragmentLifecycleOwner: LifecycleOwner?,
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    when (getHandoffViewType(handOffUI, viewModel, uiState)) {
        HandoffViewType.THREE_PL_RESTRICTED_ENABLED -> buildRestrictedThreePlEnabled(fragmentLifecycleOwner, handOffUI, viewModel, uiState)
        HandoffViewType.THREE_PL_RESTRICTED_DISABLED -> buildRestrictedThreePlDisabled(handOffUI, viewModel)
        HandoffViewType.THREE_PL_RESTRICTED_VERIFIED -> buildRestrictedThreePlVerified(handOffUI, viewModel, uiState)
        HandoffViewType.THREE_PL_UNRESTRICTED_DISABLED -> buildUnrestrictedThreePlDisabled(handOffUI, viewModel)
        HandoffViewType.THREE_PL_UNRESTRICTED_ENABLED -> buildUnrestrictedThreePlEnabled(handOffUI, viewModel)

        HandoffViewType.RX_DUG_RESTRICTED_ENABLED -> buildRestrictedDugEnabled(fragmentLifecycleOwner, handOffUI, viewModel, uiState)

        HandoffViewType.DUG_RESTRICTED_ENABLED -> buildRestrictedDugEnabled(fragmentLifecycleOwner, handOffUI, viewModel, uiState)
        HandoffViewType.DUG_RESTRICTED_DISABLED -> buildRestrictedDugDisabled(handOffUI, viewModel, uiState)
        HandoffViewType.DUG_RESTRICTED_VERIFIED -> buildRestrictedDugVerified(handOffUI, viewModel, uiState)
        HandoffViewType.DUG_UNRESTRICTED_ENABLED -> buildUnrestrictedDugEnabled(handOffUI, viewModel, uiState)
        HandoffViewType.DUG_UNRESTRICTED_DISABLED -> buildUnrestrictedDugDisabled(handOffUI, viewModel, uiState)
        else -> Unit
    }
}

fun getHandoffViewType(
    handOffUI: HandOffUI?,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
): HandoffViewType? {

    return when {
        !handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() && uiState.isVerifiedPickupPersonState() ->
            HandoffViewType.THREE_PL_RESTRICTED_VERIFIED

        !handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.THREE_PL_RESTRICTED_ENABLED
        !handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && !viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.THREE_PL_RESTRICTED_DISABLED

        !handOffUI?.isDugOrder.orFalse() && !viewModel.hasRegulatedItems.value.orFalse() && !viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.THREE_PL_UNRESTRICTED_DISABLED
        !handOffUI?.isDugOrder.orFalse() && !viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.THREE_PL_UNRESTRICTED_ENABLED

        handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() && uiState.isVerifiedPickupPersonState() ->
            HandoffViewType.DUG_RESTRICTED_VERIFIED

        handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.DUG_RESTRICTED_ENABLED
        handOffUI?.isDugOrder.orFalse() && viewModel.isRxDugHandOff.value == true -> HandoffViewType.RX_DUG_RESTRICTED_ENABLED
        handOffUI?.isDugOrder.orFalse() && viewModel.hasRegulatedItems.value.orFalse() && !viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.DUG_RESTRICTED_DISABLED

        handOffUI?.isDugOrder.orFalse() && !viewModel.hasRegulatedItems.value.orFalse() && viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.DUG_UNRESTRICTED_ENABLED
        handOffUI?.isDugOrder.orFalse() && !viewModel.hasRegulatedItems.value.orFalse() && !viewModel.digitizedAgeVerificationEnabled() -> HandoffViewType.DUG_UNRESTRICTED_DISABLED
        else -> null
    }
}

private fun GroupieAdapter.buildRestrictedThreePlDisabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
) {
    Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        add(LegacyHandOffVerificationItem(viewModel))
        updateAsync(mutableListOf(this))
    }
}

private fun GroupieAdapter.buildUnrestrictedThreePlEnabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
) {
    Section().apply {
        val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        updateAsync(mutableListOf(this))
    }
}

private fun GroupieAdapter.buildRestrictedThreePlEnabled(
    fragmentLifecycleOwner: LifecycleOwner?,
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {

    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    val beginVerificationSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        add(HandOffIdVerificationItem(viewModel))
    }

    val removeItemsSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        add(HandOffRemoveItems(fragmentLifecycleOwner, viewModel, handOffUI, uiState))
    }
    val itemsRemovedSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        add(HandOffVerificationItemsRemovedItem(viewModel))
    }
    when (uiState) {
        HandOffVerificationState.REMOVE_ITEMS -> updateAsync(mutableListOf(removeItemsSection))
        HandOffVerificationState.ITEMS_REMOVED -> updateAsync(mutableListOf(itemsRemovedSection))
        else -> updateAsync(mutableListOf(beginVerificationSection))
    }
}

private fun GroupieAdapter.buildUnrestrictedThreePlDisabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
) {
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        updateAsync(mutableListOf(this))
    }
}

private fun GroupieAdapter.buildRestrictedThreePlVerified(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    val verifyPickupPersonSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        add(HandOffIdVerifiedItem(viewModel))
    }

    if (uiState.isVerifiedPickupPersonState()) {
        updateAsync(mutableListOf(verifyPickupPersonSection))
    }
}

private fun GroupieAdapter.buildRestrictedDugEnabled(
    fragmentLifecycleOwner: LifecycleOwner?,
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val handoffVerificationCodeItem = if (uiState.isVerifyingCodeState()) HandOffVerificationCodeLoadingItem() else HandOffVerificationCode(handOffUI, viewModel)
    val showOrderVerified = viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()

    val verifyCode = Section().apply {
        val showCodeVerificationItem = (!uiState.isBeginVerificationState() && handOffUI.handshakeRequired)
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel, fragmentLifecycleOwner))
        if (showCodeVerificationItem && !handOffUI.isRxDug) {
            add(HandOffVerificationCode(handOffUI, viewModel))
        } else if (showOrderVerified && handOffUI.isRxDug) {
            add(HandOffVerificationOrderVerifiedItem(viewModel, fragmentLifecycleOwner))
        } else {
            add(handoffVerificationCodeItem)
        }
        if (viewModel.hasRegulatedItems.value.orFalse()) {
            add(HandOffIdVerificationItem(viewModel))
        }
    }

    val orderComplete = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        add(HandOffVerificationOrderVerifiedItem(viewModel, fragmentLifecycleOwner))
    }

    val removeItemsSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (handOffUI.handshakeRequired) add(HandOffVerificationOrderVerifiedItem(viewModel, fragmentLifecycleOwner))
        add(HandOffRemoveItems(fragmentLifecycleOwner, viewModel, handOffUI, uiState))
        // TODO will be removed once the redesign is completed
        // add(HandOffIdVerificationItem(viewModel))
    }
    val beginVerification = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (handOffUI.handshakeRequired) add(HandOffVerificationOrderVerifiedItem(viewModel, fragmentLifecycleOwner))
        add(HandOffIdVerificationItem(viewModel))
    }
    val itemsRemovedSection = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (handOffUI.handshakeRequired) add(HandOffVerificationOrderVerifiedItem(viewModel, fragmentLifecycleOwner))
        add(HandOffVerificationItemsRemovedItem(viewModel))
    }
    when {
        uiState.isRemoveRestrictedItemsState() || uiState.isRxRemoveRestrictedItemsState() -> updateAsync(mutableListOf(removeItemsSection))
        uiState.isItemsRemovedState() || uiState.isRxItemsRemovedState() -> updateAsync(mutableListOf(itemsRemovedSection))
        handOffUI.handshakeRequired.orFalse() &&
            !uiState.isBeginVerificationState() -> updateAsync(mutableListOf(verifyCode))

        uiState.isBeginVerificationState() -> updateAsync(mutableListOf(beginVerification))
        uiState.isRemoveRestrictedItemsState() && viewModel.isRxDugHandOff.value.orFalse() -> updateAsync(mutableListOf(orderComplete))
        else -> updateAsync(mutableListOf(beginVerification))
    }
}

private fun GroupieAdapter.buildRestrictedDugDisabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val handoffVerificationCodeItem = if (uiState.isVerifyingCodeState()) HandOffVerificationCodeLoadingItem() else HandOffVerificationCode(handOffUI, viewModel)
    val showCodeVerificationItem = !uiState.isCodeVerifiedState() && handOffUI.isAuthDugEnabled.orFalse()
    val showOrderVerified = viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()
    val showPerscriptionUnavailable = (viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()) && viewModel.isPharmacyNotServicingOrders
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (showOrderVerified) add(HandOffVerificationOrderVerifiedItem(viewModel))
        if (!showCodeVerificationItem) add(LegacyHandOffVerificationItem(viewModel))
        if (showCodeVerificationItem) add(handoffVerificationCodeItem)
        updateAsync(mutableListOf(this))
    }
}

private fun GroupieAdapter.buildRestrictedDugVerified(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val showPerscriptionUnavailable = (viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()) && viewModel.isPharmacyNotServicingOrders
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()

    val verifiedRestrictedDugVerified = Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (handOffUI.handshakeRequired) add(HandOffVerificationOrderVerifiedItem(viewModel))
        add(HandOffIdVerifiedItem(viewModel))
    }

    if (uiState.isVerifiedPickupPersonState()) {
        updateAsync(mutableListOf(verifiedRestrictedDugVerified))
    }
}

private fun GroupieAdapter.buildUnrestrictedDugEnabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val handoffVerificationCodeItem = if (uiState.isVerifyingCodeState()) HandOffVerificationCodeLoadingItem() else HandOffVerificationCode(handOffUI, viewModel)
    val showCodeVerificationItem = !uiState.isCodeVerifiedState() && handOffUI.isAuthDugEnabled.orFalse()
    val showOrderVerified = viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()
    val showPerscriptionUnavailable = (viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()) && viewModel.isPharmacyNotServicingOrders
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (showOrderVerified) add(HandOffVerificationOrderVerifiedItem(viewModel))
        if (showCodeVerificationItem) add(handoffVerificationCodeItem)
        updateAsync(mutableListOf(this))
    }
}

private fun GroupieAdapter.buildUnrestrictedDugDisabled(
    handOffUI: HandOffUI,
    viewModel: HandOffViewModel,
    uiState: HandOffVerificationState?,
) {
    val handoffVerificationCodeItem = if (uiState.isVerifyingCodeState()) HandOffVerificationCodeLoadingItem() else HandOffVerificationCode(handOffUI, viewModel)
    val showCodeVerificationItem = !uiState.isCodeVerifiedState() && handOffUI.isAuthDugEnabled.orFalse()
    val showConfirmOrderItem = !handOffUI.isAuthDugEnabled.orFalse()
    val showPerscriptionUnavailable = (viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()) && viewModel.isPharmacyNotServicingOrders
    val showOrderVerified = viewModel.isAuthCodeVerified.value.orFalse() || viewModel.authCodeIssueReported.value.orFalse()
    Section().apply {
        add(HandOffVerificationHeader(viewModel))
        add(HandOffVerificationOrderInfo(handOffUI, viewModel))
        if (showConfirmOrderItem) add(HandOffVerificationConfirmOrderItem(handOffUI, viewModel))
        if (showOrderVerified) add(HandOffVerificationOrderVerifiedItem(viewModel))
        if (showCodeVerificationItem) add(handoffVerificationCodeItem)
        updateAsync(mutableListOf(this))
    }
}

enum class HandoffViewType {
    THREE_PL_RESTRICTED_ENABLED,
    THREE_PL_RESTRICTED_DISABLED,
    THREE_PL_RESTRICTED_VERIFIED,
    THREE_PL_UNRESTRICTED_ENABLED,
    THREE_PL_UNRESTRICTED_DISABLED,
    DUG_RESTRICTED_ENABLED,
    RX_DUG_RESTRICTED_ENABLED,
    DUG_RESTRICTED_DISABLED,
    DUG_RESTRICTED_VERIFIED,
    DUG_UNRESTRICTED_ENABLED,
    DUG_UNRESTRICTED_DISABLED
}
