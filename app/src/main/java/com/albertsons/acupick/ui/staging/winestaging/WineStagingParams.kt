package com.albertsons.acupick.ui.staging.winestaging

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class WineStagingParams(
    val contactName: String,
    val activityId: String,
    val entityId: String,
    val shortOrderNumber: String,
    val customerOrderNumber: String,
    val stageByTime: String,
    val pickedUpBottleCount: String
) : Parcelable
