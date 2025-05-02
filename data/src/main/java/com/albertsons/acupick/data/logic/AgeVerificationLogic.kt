package com.albertsons.acupick.data.logic

import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.PickupType
import com.albertsons.acupick.data.model.PickupType.CUSTOMER
import com.albertsons.acupick.data.model.PickupType.DELIVERY_DRIVER
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty

object AgeVerificationLogic {

    fun ArrayList<ActivityDto>.hasActivityDetails(erId: Long) = this.any { it.erId == erId }

    fun ActivityDto.activityHasRegulatedItems() = this.containerItems?.flatMap { item ->
        item.pickedUpcCodes.orEmpty().filter {
            it.regulated == true
        }
    }.isNotNullOrEmpty()

    fun List<ActivityDto>.listHasRegulatedItems() = this.filter { it.activityHasRegulatedItems() }.isNotNullOrEmpty()

    // Should be acheivable by checking the item via isSelectableByKey
    fun List<ActivityDto>.selectedItemIsRegulated(erId: List<Long>?) = this.firstOrNull {
        it.erId == erId?.firstOrNull()
    }?.activityHasRegulatedItems() == true

    fun isRegulatedOrder(ageVerificationEnabled: Boolean, hasRestrictedItems: Boolean): Boolean {
        return ageVerificationEnabled && hasRestrictedItems
    }

    fun isVerificationCtaEnabled(handoffState: HandOffVerificationState?, handoffInfoValid: Boolean?, isVerifyCodeCtaEnabled: Boolean?): Boolean? {
        return when {
            handoffState.isVerifiedPickupPersonState() -> handoffInfoValid
            else -> isVerifyCodeCtaEnabled
        }
    }

    fun pickupType(fulfillmentType: FulfillmentType?): PickupType {
        return if (fulfillmentType == FulfillmentType.DUG) CUSTOMER else DELIVERY_DRIVER
    }

    fun HandOffVerificationState?.isBeginVerificationState() = this == HandOffVerificationState.BEGIN_VERIFICATION
    fun HandOffVerificationState?.isVerifiedPickupPersonState() = this == HandOffVerificationState.VERIFIED_PICKUP_PERSON
    fun HandOffVerificationState?.isRemoveRestrictedItemsState() = this == HandOffVerificationState.REMOVE_ITEMS
    fun HandOffVerificationState?.isItemsRemovedState() = this == HandOffVerificationState.ITEMS_REMOVED
    fun HandOffVerificationState?.isRxRemoveRestrictedItemsState() = this == HandOffVerificationState.RX_REMOVE_ITEMS
    fun HandOffVerificationState?.isRxItemsRemovedState() = this == HandOffVerificationState.RX_ITEMS_REMOVED
    fun HandOffVerificationState?.isVerifyCodeState() = this == HandOffVerificationState.VERIFY_CODE
    fun HandOffVerificationState?.isVerifyingCodeState() = this == HandOffVerificationState.VERIFYING_CODE
    fun HandOffVerificationState?.isCodeVerifiedState() = this == HandOffVerificationState.CODE_VERIFIED
    fun HandOffVerificationState?.isIdleState() = this == HandOffVerificationState.IDLE
}

enum class HandOffVerificationState {
    IDLE, VERIFY_CODE, VERIFYING_CODE, CODE_VERIFIED, BEGIN_VERIFICATION, VERIFIED_PICKUP_PERSON, REMOVE_ITEMS, ITEMS_REMOVED, RX_REMOVE_ITEMS, RX_ITEMS_REMOVED
}
