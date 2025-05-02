package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class BoxData(
    val referenceEntityId: String,
    val zoneType: StorageType,
    val type: String,
    val orderNumber: String,
    val boxNumber: String,
    val label: String
) : Parcelable, Dto
