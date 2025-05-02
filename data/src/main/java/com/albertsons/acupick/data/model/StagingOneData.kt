package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class Tote(
    val customerOrderNumber: String,
    val toteId: String,
    val bagCount: Int?,
    val looseItemCount: Int?,
    val storageType: StorageType?,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class StagingOneData(
    var savedTitle: String,
    val activityId: Long,
    var nextActivityId: Long?,
    var bagLabelsPrintedSuccessfully: Boolean,
    var totesByToteIdMap: Map<String, Tote>,
    var isMultiSource: Boolean,
) : Parcelable, Dto
