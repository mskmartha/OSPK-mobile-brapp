package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.response.ItemAddressDto

fun ItemAddressDto.asItemLocation(context: Context): String {
    return if (this.side == "00" && this.bay == "00" && this.level == "00") {
        deptShortName.orEmpty()
    } else {
        val aisle = this.aisleSeq ?: context.getString(R.string.item_location_fallback_aisle)
        val side = this.side ?: context.getString(R.string.item_location_fallback_side)
        val bay = this.bay ?: context.getString(R.string.item_location_fallback_bay)
        val level = this.level ?: context.getString(R.string.item_location_fallback_level)
        val itemLocationCode = "$aisle-$side-$bay-$level"
        itemLocationCode
    }
}
