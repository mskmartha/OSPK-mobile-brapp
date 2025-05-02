package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.OrderCountByStoreDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the AppSummaryResponseDto swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class AppSummaryResponseDto(
    @Json(name = "isAutoInitiated") val isAutoInitiated: Boolean? = null,
    @Json(name = "activity") val activity: ActivityAndErDto? = null,
    @Json(name = "orderCountByStore") val orderCountByStore: List<OrderCountByStoreDto>? = null,
) : Parcelable, Dto

fun AppSummaryResponseDto.isAssigned(userId: String?): Boolean = activity?.assignedTo?.userId == userId
