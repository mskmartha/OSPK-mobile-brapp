package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the InventoryAttribute swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class InventoryAttributeDto(
    @Json(name = "coo") val coo: String? = null,
    @Json(name = "expiryDate") val expiryDate: ZonedDateTime? = null,
    @Json(name = "serialNumber") val serialNumber: String? = null,
    @Json(name = "state") val state: State? = null,
    @Json(name = "supplyBucket") val supplyBucket: SupplyBucket? = null,
    @Json(name = "tag") val tag: String? = null
) : Parcelable, Dto {

    @JsonClass(generateAdapter = false)
    enum class State {
        @Json(name = "GOOD") GOOD,
        @Json(name = "DAMAGED") DAMAGED,
        @Json(name = "RETURNED") RETURNED
    }

    @JsonClass(generateAdapter = false)
    enum class SupplyBucket {
        @Json(name = "ON_HAND") ON_HAND,
        @Json(name = "PLANNED") PLANNED,
        @Json(name = "X_DOCK") X_DOCK
    }
}
