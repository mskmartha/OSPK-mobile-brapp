package com.albertsons.acupick.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SellByType(
    val code: String,
) {
    @Json(name = "E")
    Each(
        code = "E"
    ),
    @Json(name = "W")
    Weight(
        code = "W"
    ),
    @Json(name = "I")
    RegularItem(
        code = "I"
    ),
    @Json(name = "P")
    Prepped(
        code = "P"
    ),
    @Json(name = "PEU")
    PriceEachUnique(
        code = "PEU"
    ),
    @Json(name = "PS")
    PriceScaled(
        code = "PS"
    ),
    @Json(name = "PET")
    PriceEachTotal(
        code = "PET"
    ),
    @Json(name = "PE")
    PriceEach(
        code = "PE"
    ),
    @Json(name = "PW")
    PriceWeighted(
        code = "PW"
    ),
}

fun SellByType.thatNeedsToBeSplit() =
    this == SellByType.Prepped ||
        this == SellByType.PriceEachUnique ||
        this == SellByType.PriceEachTotal ||
        this == SellByType.PriceScaled
