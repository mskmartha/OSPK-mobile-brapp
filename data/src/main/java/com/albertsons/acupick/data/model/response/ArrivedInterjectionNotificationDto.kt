package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ArrivedInterjectionNotificationDto(
    @Json(name = "customerArrivedTime") val customerArrivedTime: ZonedDateTime? = null,
) : Parcelable, Dto
