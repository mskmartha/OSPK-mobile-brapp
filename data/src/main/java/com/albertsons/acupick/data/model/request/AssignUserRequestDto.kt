package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class AssignUserRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "replaceOverride") val replaceOverride: Boolean? = null,
    @Json(name = "user") val user: UserDto? = null,
    @Json(name = "defaultPickListSelected") val defaultPickListSelected: Boolean = false,
    @Json(name = "tokenizedLdapId") val tokenizedLdapId: String? = null
) : Parcelable, Dto
