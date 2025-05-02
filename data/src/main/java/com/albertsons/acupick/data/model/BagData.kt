package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class BagData(
    val bagId: String,
    val zoneType: StorageType,
    val customerOrderNumber: String?,
    val fulfillmentOrderNumber: String?,
    val contactFirstName: String?,
    val contactLastName: String?,
    val containerId: String,
    val isBatch: Boolean,
) : Parcelable, Dto
