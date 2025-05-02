package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ContactPerson swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ContactPersonDto(
    @Json(name = "emailId") val emailId: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
) : Parcelable, Dto {
    fun getFullName() = "$firstName $lastName"
}
