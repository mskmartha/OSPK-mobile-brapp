package com.albertsons.acupick.ui.arrivals.pharmacy

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.ui.arrivals.complete.HandOffUI
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class PrescriptionReturnData(
    val scannedData: List<String>,
    val handOffUI: List<HandOffUI> = emptyList(),
    val isFromNotification: Boolean = false,
    val fromPartialPrescriptionPickup: Boolean = false
) : Parcelable
