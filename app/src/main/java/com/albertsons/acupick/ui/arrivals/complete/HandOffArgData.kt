package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.response.OrderSummary
import com.albertsons.acupick.ui.models.CustomerInfo
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class HandOffArgData(
    val handOffUIList: List<HandOffUI>,
    val currentHandOffUI: HandOffUI? = null,
    val currentHandOffResultData: HandOffResultData? = null,
    val customerData: CustomerInfo? = null,
    val currentHandOffIndex: Int = 0,
    val customerOrders: String? = null,
) : Parcelable

@Parcelize
@Keep
data class OrderSummaryArg(
    val isCas: Boolean?,
    val orderSummary: List<OrderSummary>,
    val is3p: Boolean?,
    val source: String?
) : Parcelable
