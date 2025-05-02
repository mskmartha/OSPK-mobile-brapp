package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class RxRemoveRequestDto(
    val orderId: String?,
    val storeNumber: String?,
    val orderStatus: String?,
    val cartType: CartType?,
    val rxBagReturnScanTimestamp: ZonedDateTime?,
    val rxLocationReturnScanTimestamp: ZonedDateTime?,
    val rxReturnCompleteTimestamp: ZonedDateTime?,
    val rxOrders: List<RxOrder>?,
) : Parcelable, Dto
