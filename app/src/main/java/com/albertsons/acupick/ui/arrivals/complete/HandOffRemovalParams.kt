package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HandOffRemovalParams(
    val isRx: Boolean,
    val handOffUI: HandOffUI?,
) : Parcelable
