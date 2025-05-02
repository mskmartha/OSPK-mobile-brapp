package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the User swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class UserDto(
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "userId") val userId: String? = null
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class UserActivityLoginDto(
    @Json(name = "userActivityId") val userActivityId: Long? = null,
) : Parcelable, Dto

fun UserDto.firstInitialDotLastName(): String {
    val firstInitial = this.firstName?.substring(0, 1)
    val lastName = this.lastName
    return "$firstInitial. $lastName"
}
fun UserDto.firstNameLastInitialDot(): String {
    val firstName = this.firstName
    val lastInitial = this.lastName?.substring(0, 1)?.uppercase()
    return "$firstName $lastInitial."
}
