package com.albertsons.acupick.ui.substitute

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerationsclass
data class SubstituteParams(
    /** Maps to [com.albertsons.acupick.data.model.response.ItemActivityDto.id] */
    val iaId: Long?,
    val pickListId: String,
    val path: SubstitutionPath? = SubstitutionPath.PICKING,
    val substitutionRemovedQty: Int? = null,
    val messageSid: String? = null,
    val swapSubstitutionReason: SwapSubstitutionReason? = null
) : Parcelable

/**
 * This path is used to distinguish the normal substitution from swap substitution flow
 */
@Parcelize
@Keep
enum class SubstitutionPath : Parcelable {
    PICKING,
    SWAPSUBSTITUTION,
    REPICK_ORIGINAL_ITEM
}

@Parcelize
@Keep
enum class SwapSubstitutionReason : Parcelable {
    SWAP,
    SWAP_OOS,
    SWAP_OTHER_PICKLIST,
    SWAP_OOS_OTHER_PICKLIST
}

fun SubstitutionPath?.isRepickOriginalItem(): Boolean = this == SubstitutionPath.REPICK_ORIGINAL_ITEM

fun SwapSubstitutionReason.isSwapSubstitutionForOtherPicker(): Boolean = this == SwapSubstitutionReason.SWAP_OTHER_PICKLIST || this == SwapSubstitutionReason.SWAP_OOS_OTHER_PICKLIST
fun SwapSubstitutionReason.isSwapSubstitutionForMyItems(): Boolean = this == SwapSubstitutionReason.SWAP || this == SwapSubstitutionReason.SWAP_OOS

fun SwapSubstitutionReason?.getSubstituteRejectionReasonValue(): SubstitutionRejectedReason? {
    return when (this) {
        SwapSubstitutionReason.SWAP -> SubstitutionRejectedReason.SWAP
        SwapSubstitutionReason.SWAP_OOS -> SubstitutionRejectedReason.SWAP_OOS
        SwapSubstitutionReason.SWAP_OTHER_PICKLIST -> SubstitutionRejectedReason.SWAP_OTHER_PICKLIST
        SwapSubstitutionReason.SWAP_OOS_OTHER_PICKLIST -> SubstitutionRejectedReason.SWAP_OOS_OTHER_PICKLIST
        else -> null
    }
}
