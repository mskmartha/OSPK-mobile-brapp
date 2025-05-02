package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@Keep
@Parcelize
data class RxBagUI(
    val orderNumber: String?,
    val bagNumber: String?,
    val deliveryFailReason: String?,
    val rxReturnBagScanTimestamp: ZonedDateTime?
) : Parcelable
