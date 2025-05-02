package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ItemPickWrapperReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemPickRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "lineReq") val lineReqDto: List<LineRequestDto>? = null
) : Parcelable, Dto

/** Syntax sugar to pull out pickedTime from the first item in the [ItemPickRequestDto.lineReqDto] list (or null if not present) */
val ItemPickRequestDto.pickedTime: ZonedDateTime?
    get() = lineReqDto?.firstOrNull()?.pickedTime
