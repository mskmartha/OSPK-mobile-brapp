package com.albertsons.acupick.data.model.request

import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import android.os.Parcelable
import com.albertsons.acupick.data.model.User
import kotlinx.parcelize.Parcelize
import java.time.Instant

@JsonClass(generateAdapter = true)
@Parcelize
data class UserActivityRequestDto(
    @Json(name = "user") val user: UserActivityRequestUser,
    @Json(name = "deviceType") val deviceType: String,
    @Json(name = "loginTime") val loginTime: String,
    @Json(name = "logoutTime") val logoutTime: String? = null,
    @Json(name = "siteId") val siteId: String,
    @Json(name = "logoutReason") val logoutReason: String? = null,
) : Parcelable, Dto {
    @JsonClass(generateAdapter = true)
    @Parcelize
    data class UserActivityRequestUser(
        @Json(name = "firstName") val firstName: String,
        @Json(name = "lastName") val lastName: String,
        @Json(name = "userId") val userId: String,
    ) : Parcelable

    companion object {
        private const val ACTIVITY_DEVICE_TYPE = "ANDROID"
        const val ACTIVITY_LOGOUT_REASON_USER_INITIATED = "USER_INITIATED"
        const val ACTIVITY_LOGOUT_REASON_APP_TIMEOUT = "APP_TIMEOUT"
        const val ACTIVITY_LOGOUT_REASON_CHANGED_STORE = "CHANGED_STORE"

        fun generateLoginDto(loginTime: Instant, siteId: String, user: User?) = UserActivityRequestDto(
            deviceType = ACTIVITY_DEVICE_TYPE,
            loginTime = loginTime.toString(),
            siteId = siteId,
            user = UserActivityRequestUser(
                firstName = user?.firstName ?: "",
                lastName = user?.lastName ?: "",
                userId = user?.userId ?: ""
            )
        )

        fun generateLogoutDto(loginTime: Instant, logoutTime: Instant, user: User?, logoutReason: String) = UserActivityRequestDto(
            deviceType = ACTIVITY_DEVICE_TYPE,
            loginTime = loginTime.toString(),
            logoutTime = logoutTime.toString(),
            logoutReason = logoutReason,
            siteId = user?.selectedStoreId ?: "",
            user = UserActivityRequestUser(
                firstName = user?.firstName ?: "",
                lastName = user?.lastName ?: "",
                userId = user?.userId ?: ""
            )
        )
    }
}
