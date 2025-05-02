package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ImageSizePreset
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.barcode.BarcodeMapper
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.isIssueScanned
import com.albertsons.acupick.data.model.response.isSubstituted
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.ui.picklistitems.PickListType

// When aisleSeq in itemAddress contains this value, the app should display the department name as the item location
private const val AISLE_SEQ_PRODUCE = "PR"

fun ItemActivityDto?.asItemLocation(context: Context, showDeptName: Boolean = true): String {
    val location = this?.itemAddressDto

    return if (location == null && showDeptName) {
        this?.depName ?: this?.deptName ?: context.getString(R.string.unavailable)
    } else if (location == null) {
        ""
        // context.getString(R.string.unavailable)
    } else if (location.side == "00" && location.bay == "00" && location.level == "00") {
        this?.depName ?: this?.deptName.orEmpty()
    } else {
        val aisle = location.aisleSeq ?: context.getString(R.string.item_location_fallback_aisle)
        val side = location.side ?: context.getString(R.string.item_location_fallback_side)
        val bay = location.bay ?: context.getString(R.string.item_location_fallback_bay)
        val level = location.level ?: context.getString(R.string.item_location_fallback_level)
        val itemLocationCode = "$aisle-$side-$bay-$level"
        itemLocationCode
    }
}

fun ItemActivityDto?.asUpcOrPlu(context: Context, barcodeMapper: BarcodeMapper) = when (this?.sellByWeightInd) {
    SellByType.Each -> pluFormatting(this, context)
    SellByType.Weight -> {
        if (this.pluList?.getOrNull(0)?.toIntOrNull() == null || this.pluList?.getOrNull(0)?.toIntOrNull() == 0) {
            ""
        } else {
            if (this.isOrderedByWeight()) {
                context.getString(
                    R.string.item_details_plu_weighted_format, this.pluList?.getOrNull(0), (this.orderedWeightUOM ?: context.getString(R.string.uom_default)).uppercase()
                )
            } else {
                context.getString(R.string.item_details_plu_format, this.pluList?.getOrNull(0))
            }
        }
    }
    SellByType.RegularItem -> upcFormatting(this, context, barcodeMapper)
    SellByType.Prepped, SellByType.PriceEachTotal, SellByType.PriceEachUnique, SellByType.PriceEach, SellByType.PriceScaled,
    SellByType.PriceWeighted -> context.getString(R.string.item_details_slu_format, getDisplaySlu())
    else -> ""
}

private fun upcFormatting(item: ItemActivityDto, context: Context, barcodeMapper: BarcodeMapper) =
    if (item.primaryUpc.isNullOrBlank()) {
        ""
    } else {
        context.getString(R.string.item_details_upc_format, barcodeMapper.generateDisplayBarCode(item.primaryUpc))
    }

private fun pluFormatting(item: ItemActivityDto, context: Context) =
    if (item.pluList?.getOrNull(0)?.toIntOrNull() == null || item.pluList?.getOrNull(0)?.toIntOrNull() == 0) {
        ""
    } else {
        context.getString(R.string.item_details_plu_format, item.pluList?.getOrNull(0))
    }

// SLU is the 5 digits after "002" in the UPC
fun ItemActivityDto.getDisplaySlu(): String = primaryUpc?.replace("002", "")?.take(5).orEmpty()

fun ItemActivityDto.sizedImageUrl(imageSize: ImageSizePreset): String =
    getSizedImageUrl(imageUrl, imageSize)

fun ItemDetailDto.sizedImageUrl(imageSize: ImageSizePreset): String = getSizedImageUrl(imageUrl, imageSize)

fun getSizedImageUrl(imageUrl: String?, imageSize: ImageSizePreset) = if (imageUrl == null) "" else imageUrl + "?" + imageSize.suffix + "&defaultImage=Not_Available"

fun ItemActivityDto?.asCustomerComments(context: Context): String {
    val instructions = this?.instructionDto?.text.orEmpty().trim()
    return if (instructions.isNotNullOrEmpty()) "\"$instructions\""
    else context.getString(R.string.no_user_comments)
}

fun ItemActivityDto.asSubstitutionInfo(context: Context): String =
    subCode.fetchSubstitutionString(context).let {
        if (it.isNotNullOrEmpty()) context.getString(R.string.comments_format, it) else ""
    }

fun ItemActivityDto.asSuggestedItemHeader(context: Context): String =
    subCode.fetchSuggestedItemHeaderString(context)

/** Creates a status string to display on the completed item activity card that includes relevant substitution/short info (separated with / and limited to two with trailing ... if > 2 statuses */
fun ItemActivityDto.asStatusPillString(context: Context, listType: PickListType? = null): Pair<String, Int> {
    val emptyColorPair = "" to R.color.white
    val pickedStringColorPair = when {
        isIssueScanned -> context.getString(R.string.issue_reported_label) to R.color.semiLightRed
        qty != processedQty && !isSubstituted -> context.getString(R.string.partially_picked) to R.color.semiLightRed
        isSubstituted && isPartiallyPicked() && isFullyShorted().not() && isFullySubstituted().not() -> context.getString(R.string.partially_substituted) to R.color.semiLightRed
        isSubstituted -> context.getString(R.string.item_complete_substituted) to R.color.semiLightRed
        else -> "" to R.color.white
    }

    val shortStringColorPair = if (exceptionQty.orZero() > 0) {
        shortedItemUpc?.firstOrNull()?.exceptionReasonCode?.displayStringWithColor(context) ?: emptyColorPair
    } else {
        emptyColorPair
    }

    return when (listType) {
        PickListType.Picked -> pickedStringColorPair
        PickListType.Short -> shortStringColorPair
        else -> emptyColorPair
    }
}

/**
 * ISSUE-SCANNING This method is used to format weight unit such as 2.0-> 2, 3.3-> 3.3
 */
fun formattedWeight(weight: Double?): String = (if (((weight) ?: 0.0) % 1 != 0.0) weight ?: 0.0 else weight?.toInt()).toString()
