package com.albertsons.acupick.data.model.request

import com.albertsons.acupick.data.model.DomainModel

data class LogoutRequest(
    val refreshToken: String,
    val userId: String,
) : DomainModel {
    internal fun toLogoutRequestDto() = LogoutRequestDto(
        refreshToken = refreshToken,
        appId = "APS_APP",
        userId = userId,
    )
}
