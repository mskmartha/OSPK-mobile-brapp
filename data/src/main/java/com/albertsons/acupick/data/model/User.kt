package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.request.UserDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale

/** Represents a logged in user */
@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "userId") val userId: String,
    @Json(name = "firstName") val firstName: String,
    @Json(name = "lastName") val lastName: String,
    @Json(name = "storeIds") val sites: List<SiteDto>,
    @Json(name = "selectedStoreId") val selectedStoreId: String? = null,
    @Json(name = "tokenizedLdapId") val tokenizedLdapId: String? = null,
    val userActivityId: Long? = null,
) {
    fun toUserDto(): UserDto? {
        return UserDto(firstName = firstName, lastName = lastName, userId = userId)
    }

    fun getStoreIds(): List<String> {
        return sites.mapNotNull { it.siteId }
    }

    fun firstInitialLastName(): String {
        val firstInitial = if (firstName.isNotNullOrEmpty()) this.firstName.substring(0, 1).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else ""
        val lastName = if (lastName.isNotNullOrEmpty()) this.lastName.substring(0, 1).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else ""
        return "$firstInitial$lastName"
    }
}
