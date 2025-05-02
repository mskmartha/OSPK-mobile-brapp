package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the Address swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class AddressDto(
    @Json(name = "area") val area: String? = null,
    @Json(name = "buildingId") val buildingId: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "invoiceAddress") val invoiceAddress: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "pickUpBay") val pickUpBay: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "street") val street: String? = null,
    @Json(name = "timeZone") val timeZone: String? = null,
    @Json(name = "type") val type: Type? = null,
    @Json(name = "unitId") val unitId: String? = null
) : Parcelable, Dto {

    @JsonClass(generateAdapter = false)
    enum class Type {
        @Json(name = "RECEPTION") RECEPTION,
        @Json(name = "DOOR") DOOR,
        @Json(name = "MAILBOX") MAILBOX,
        @Json(name = "DROP_BOX") DROP_BOX,
        @Json(name = "WAREHOUSE") WAREHOUSE,
        @Json(name = "FACTORY") FACTORY,
        @Json(name = "STORE") STORE,
        @Json(name = "SHIPTO") SHIPTO
    }
}
