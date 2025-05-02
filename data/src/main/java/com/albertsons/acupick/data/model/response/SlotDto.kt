package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.infrastructure.utils.AM_PM_TIME_FORMATTER
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the Slot swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class SlotDto(
    @Json(name = "endTime") val endTime: ZonedDateTime? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "startTime") val startTime: ZonedDateTime? = null,
    @Json(name = "deliveryInstruction") val deliveryInstruction: String? = null
) : Parcelable, Dto {

    fun getFulfillmentTime() = "${startTime?.format(AM_PM_TIME_FORMATTER)} - ${endTime?.format(AM_PM_TIME_FORMATTER)}"
}
