package com.albertsons.acupick.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class RxBag(
    val orderNumber: String?,
    val bagNumber: String?,
) : Parcelable
