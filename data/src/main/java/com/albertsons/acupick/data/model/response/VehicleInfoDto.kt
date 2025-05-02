package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class VehicleInfoDto(
    @Json(name = "color") val color: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "parkedSpot") val parkedSpot: String? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "vehicleDetail") val vehicleDetail: String? = null
) : Parcelable, Dto

enum class VehicleColour(val color: String) {
    RED("red"),
    BLUE("blue"),
    BROWN("brown"),
    BEIGE("beige"),
    GREEN("green"),
    GRAY("gray"),
    SILVER("silver"),
    BLACK("black"),
    WHITE("white")
}

enum class VehicleType(val type: String) {
    CAR("car"),
    SUV("suv"),
    SEDAN("sedan"),
    VAN("van"),
    TRUCK("truck")
}
