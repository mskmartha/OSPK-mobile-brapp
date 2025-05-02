package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class WineStagingTwoData(
    var scannedBagList: List<ScannedBoxData>,
    val customerOrderNumber: String,
    val activityId: Long,
    val unassignedToteIdList: List<String>,
    val isMultiSource: Boolean,
) : Parcelable, Dto
