package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the SearchCustomerPickUpOrders swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class SearchCustomerPickUpOrdersRequestDto(
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "firstName") val firstName: String? = null,
    @Json(name = "lastName") val lastName: String? = null,
    @Json(name = "orderNo") val orderNo: String? = null
) : Parcelable, Dto
