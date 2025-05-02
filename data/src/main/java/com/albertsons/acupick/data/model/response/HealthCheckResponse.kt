package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

// Health check response model
@JsonClass(generateAdapter = true)
@Parcelize
data class HealthCheckResponse(
    @Json(name = "status") val status: String?,
    val statusCode: Int?,
    val message: String?
) : Parcelable
