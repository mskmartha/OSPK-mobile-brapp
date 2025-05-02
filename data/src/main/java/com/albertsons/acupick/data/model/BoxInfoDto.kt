package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddCountResponseDto(val actId: Int?, val nextActivityId: Int?)

@JsonClass(generateAdapter = true)
data class BoxInfoDto(
    val activityId: Int? = null,
    val boxDetails: List<BoxDetailsDto>? = null
)

@JsonClass(generateAdapter = true)
data class BoxDetailsDto(
    val referenceEntityId: String? = null,
    val boxNumber: String? = null,
    val orderNumber: String? = null,
    val label: String? = null,
    val weight: Float? = null,
    val type: BoxTypeDto? = null
)

fun BoxTypeDto.toDomain() = when (this) {
    BoxTypeDto.XS -> BoxType.XS
    BoxTypeDto.SS -> BoxType.SS
    BoxTypeDto.MM -> BoxType.MM
    BoxTypeDto.LL -> BoxType.LL
    BoxTypeDto.XL -> BoxType.XL
}

@JsonClass(generateAdapter = false)
enum class BoxTypeDto(val type: String) {
    @Json(name = "XS") XS("XS"),
    @Json(name = "SS") SS("SS"),
    @Json(name = "MM") MM("MM"),
    @Json(name = "LL") LL("LL"),
    @Json(name = "XL") XL("XL")
}
