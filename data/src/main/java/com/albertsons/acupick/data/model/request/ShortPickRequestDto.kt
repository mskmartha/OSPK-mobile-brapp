package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the ShortPickReq swagger api
 *
 * Note that containerNo was sent historically but is not relevant to online/offline shorts. It was only sent if an item was partially picked prior to being shorted.
 * See https://confluence.safeway.com/display/AcuPick/Record+Shortage?focusedCommentId=97796051#comment-97796051
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ShortPickRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "shortReq") val shortReqDto: List<ShortRequestDto>? = null
) : Parcelable, Dto

/** Syntax sugar to pull out pickedTime from the first item in the [ShortPickRequestDto.shortedTime] list (or null if not present) */
val ShortPickRequestDto.shortedTime: ZonedDateTime?
    get() = shortReqDto?.firstOrNull()?.shortedTime
