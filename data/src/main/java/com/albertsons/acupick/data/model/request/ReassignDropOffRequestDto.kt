package com.albertsons.acupick.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.Dto

@Keep
@JsonClass(generateAdapter = true)
data class ReassignDropOffRequestDto(
    @Json(name = "actId")
    val actId: Long,
    @Json(name = "user")
    val user: UserDto
) : Dto
