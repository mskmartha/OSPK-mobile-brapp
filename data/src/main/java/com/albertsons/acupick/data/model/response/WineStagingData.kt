package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.BoxData
import com.albertsons.acupick.data.model.ScannedBoxData
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Keep
@Parcelize
data class WineStagingData(
    val activityId: Int?,
    val entityId: String?,
    val shorOrderId: String?,
    val bottleCount: Int? = 0,
    val boxCount: Int? = 0,
    val stageByTime: String?,
    val customerOrderNumber: String?,
    val contactName: String?,
    val nextActivityId: WineStagingType = WineStagingType.WineStaging1,
    val boxInfo: List<BoxData>? = emptyList(),
    val scannedBoxes: List<ScannedBoxData>? = emptyList()
) : Parcelable

@Keep
@JsonClass(generateAdapter = false)
enum class WineStagingType {
    WineStaging1, WineStaging2, WineStaging3
}
