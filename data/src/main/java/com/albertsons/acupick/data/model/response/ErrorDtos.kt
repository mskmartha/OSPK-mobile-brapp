package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.net.HttpURLConnection

sealed class KnownBackendErrorTypes

/** Corresponds to the sample error Aps endpoints response. See https://confluence.safeway.com/display/EOM/Error+Codes+and+Response and [SAMPLE_SERVER_ERROR_RESPONSE_JSON] */
@JsonClass(generateAdapter = true)
@Parcelize
data class ServerErrorDto(
    // @Json(name = "allErrors") val allErrors: , // TODO: Add proper type once we know what this is
    @Json(name = "debugMessage") val debugMessage: String? = null,
    @Json(name = "errorCode") val errorCode: ServerErrorCodeDto? = null,
    /** Note that this value is set by the retrofit response rather than being parsed from the backend */
    val httpErrorCode: Int? = null,
    @Json(name = "message") val message: String? = null,
    /** Note that this value is set by the retrofit response rather than being parsed from the backend */
    val status: String? = null,
) : Parcelable, KnownBackendErrorTypes()

/** True when [ServerErrorDto.httpErrorCode] is [HttpURLConnection.HTTP_UNAUTHORIZED] or [HttpURLConnection.HTTP_FORBIDDEN] */
fun ServerErrorDto.isAuthenticationError(): Boolean = httpErrorCode == HttpURLConnection.HTTP_UNAUTHORIZED || httpErrorCode == HttpURLConnection.HTTP_FORBIDDEN

private const val SAMPLE_SERVER_ERROR_RESPONSE_JSON =
    """
{
    "status": "BAD_REQUEST",
    "httpErrorcode": 400,
    "message": "item activity 4393 does not belong to activity id 1700",
    "errorCode": 22,
    "debugMessage": null,
    "allErrors": null
}"""

/** Error format seen being returned from Auth endpoint ldapToken api. See [SAMPLE_LOGIN_ERROR_RESPONSE_JSON] */
@JsonClass(generateAdapter = true)
@Parcelize
data class AuthApiLdapLoginError(
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
) : Parcelable, KnownBackendErrorTypes()

fun AuthApiLdapLoginError.toServerErrorDto(): ServerErrorDto {
    return ServerErrorDto(message = errorDescription)
}

private const val SAMPLE_LOGIN_ERROR_RESPONSE_JSON =
    """
{
    "error": "unauthorized",
    "error_description": "Bad credentials"
}"""

/**
 * Error format seen being returned from Aps/Auth endpoint apis if something like path is wrong/api is not present/etc.
 *
 * See [SAMPLE_FALLBACK_ERROR_RESPONSE_JSON]
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class FallbackApiError(
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "path") val path: String? = null,
) : Parcelable, KnownBackendErrorTypes()

fun FallbackApiError.toServerErrorDto(): ServerErrorDto {
    return ServerErrorDto(message = message, httpErrorCode = status, status = error)
}

private const val SAMPLE_FALLBACK_ERROR_RESPONSE_JSON =
    """
{
    "timestamp": "2020-10-01T20:55:26.490+0000",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Refresh Token Not valid!!",
    "path": "/refreshToken"
}"""
