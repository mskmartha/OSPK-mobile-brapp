package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class CustomerData(val customerInfoList: List<CustomerInfo>?) : Parcelable

@Parcelize
@Keep
data class CustomerInfo(val name: String, val customerId: String?, val erid: Long) : Parcelable
