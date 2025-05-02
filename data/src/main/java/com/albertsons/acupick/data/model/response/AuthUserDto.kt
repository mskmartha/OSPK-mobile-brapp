package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AuthUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "banners") val banners: List<String>? = null,
    @Json(name = "sites") val sites: List<SiteDto>? = null,
    @Json(name = "roles") val roles: List<String>? = null,
    @Json(name = "permissions") val permissions: List<String>? = null,
    @Json(name = "tokenizedLdapId") val tokenizedLdapId: String? = null
) : Parcelable, Dto {

    internal fun toUser() = User(
        userId = userId ?: "",
        firstName = firstName ?: "",
        lastName = lastName ?: "",
        sites = sites?.map { SiteDto(it.siteId ?: "", it.isDefault ?: false) } ?: listOf(),
        selectedStoreId = if (sites?.size == 1) {
            sites.first().siteId ?: ""
        } else {
            null
        },
        tokenizedLdapId = tokenizedLdapId
    )
}
