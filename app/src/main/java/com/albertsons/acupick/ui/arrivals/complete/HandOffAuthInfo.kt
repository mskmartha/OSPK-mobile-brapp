package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class HandOffAuthInfo(
    val authCode: String? = null,
    val customerName: String? = null
) : Parcelable
