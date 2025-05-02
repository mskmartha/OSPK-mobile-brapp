package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Parcelize
@JsonClass(generateAdapter = true)
data class ItemPickCompleteDto(
    val iaId: Long?,
    val siteId: String?,
    val messageSid: String?,
    val substitutedItems: List<SubstitutedItems>?,
) : Parcelable, Dto

@Parcelize
@JsonClass(generateAdapter = true)
data class SubstitutedItems(
    val itemId: String?,
    val pickedUpc: String?,
    @Transient val substitutedTime: ZonedDateTime? = null,
) : Parcelable, Dto
