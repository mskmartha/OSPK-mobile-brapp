package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.albertsons.acupick.infrastructure.utils.getFormattedWithAmPm
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the FulfillmentSlot swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class FulfillmentSlotDto(
    @Json(name = "startTime") val startTime: ZonedDateTime? = null,
    @Json(name = "endTime") val endTime: ZonedDateTime? = null,
    @Json(name = "name") val name: String? = null
) : Parcelable, Dto {

    fun getFulfilmentTime() = "${getFormattedWithAmPm(startTime)} -  ${getFormattedWithAmPm(endTime)}"
}
