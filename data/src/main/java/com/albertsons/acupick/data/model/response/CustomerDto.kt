package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the Customer swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class CustomerDto(
    @Json(name = "emailId") val emailId: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "segment") val segment: Segment? = null,
    @Json(name = "type") val type: Type? = null,
) : Parcelable, Dto {

    @JsonClass(generateAdapter = false)
    enum class Segment {
        @Json(name = "REGULAR") REGULAR,
        @Json(name = "PRIME") PRIME,
        @Json(name = "PLATINUM") PLATINUM,
        @Json(name = "GOLD") GOLD,
        @Json(name = "INTERNAL") INTERNAL,
    }

    @JsonClass(generateAdapter = false)
    enum class Type {
        @Json(name = "REGULAR") REGULAR,
        @Json(name = "EMPLOYEE") EMPLOYEE,
        @Json(name = "BUSINESS") BUSINESS,
        @Json(name = "GOVERNMENT") GOVERNMENT,
        @Json(name = "INVENTORY") INVENTORY,
    }
}
