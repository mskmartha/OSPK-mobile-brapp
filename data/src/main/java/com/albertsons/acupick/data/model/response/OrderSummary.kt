package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class OrderSummary(
    val imageUrl: String? = null,
    val itemDesc: String? = null,
    val qty: Double? = 0.0,
    val price: Double? = null,
    val upcId: String? = null,
    val title: Title? = null,
    val substitutedWith: List<OrderSummary>? = null,
) : Parcelable {

    val quantity = qty?.toInt().toString()
    val showPrice = price != null
}
