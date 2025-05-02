package com.albertsons.acupick.data.model.response
import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class StoreUsersLdapDTO(
    @Json(name = "siteId") val siteId: String,
    @Json(name = "users") val storeUserDetails: List<StoreUserDetails>
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class StoreUserDetails(
    @Json(name = "tokenizedLdapId") val tokenizedLdapId: String,
    @Json(name = "firstName") val firstName: String? = null
) : Parcelable, Dto
