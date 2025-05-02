package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class RemovedItemsAnalytics(
    @Json(name = "storageType")
    val storageType: StorageType? = null,
    @Json(name = "quantity")
    val quantity: Int? = null,
    @Json(name = "startTimestamp")
    val startTimestamp: ZonedDateTime? = null,
    @Json(name = "endTimestamp")
    val endTimestamp: ZonedDateTime? = null,
) : Parcelable
