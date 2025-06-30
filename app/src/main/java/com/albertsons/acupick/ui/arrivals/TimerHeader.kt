package com.albertsons.acupick.ui.arrivals

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class TimerHeaderData(
    val totalTime:Long = 280L,
    val elapsedTime : Long = 0L,
) : Parcelable