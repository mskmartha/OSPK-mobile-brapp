package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = false)
enum class RxOrderStatus {
    @Json(name = "SCANNED")
    SCANNED,
    @Json(name = "DELIVERY_FAILED")
    DELIVERY_FAILED,
    @Json(name = "DELIVERY_FAILED_NO_PICKUP")
    DELIVERY_FAILED_NO_PICKUP, // helps identify internal state
    @Json(name = "DELIVERY_COMPLETED")
    DELIVERY_COMPLETED,
}

@JsonClass(generateAdapter = false)
enum class RxDeliveryFailedReason(val value: String) {
    @Json(name = "PHARMACY_CLOSED")
    PHARMACY_CLOSED("Pharmacy closed"),
    @Json(name = "ORDER_NOT_READY_FOR_PICKUP")
    ORDER_NOT_READY_FOR_PICKUP("Order is not ready for pickup"),
    @Json(name = "CUSTOMER_UNABLE_TO_PROVIDE_OTP_CODE")
    CUSTOMER_UNABLE_TO_PROVIDE_OTP_CODE("Customer unable to provide OTP Code"),
    @Json(name = "PHARMACY_STAFF_NOT_PRESENT")
    PHARMACY_STAFF_NOT_PRESENT("Pharmacy staff not present"),
    @Json(name = "PHARMACY_SYSTEM_CANNOT_PROCESS")
    PHARMACY_SYSTEM_CANNOT_PROCESS("Pharmacy system cannot process"),
    @Json(name = "BAG_PROCESSED_SUCCESSFULLY")
    BAG_PROCESSED_SUCCESSFULLY("Bag processed successfully"),
    @Json(name = "BAG_FAILED_TO_PROCESS")
    BAG_FAILED_TO_PROCESS("Bag failed to process"),
}

@JsonClass(generateAdapter = true)
@Parcelize
data class RxOrder(
    val rxOrderId: String?,
    val rxOrderStatus: RxOrderStatus?,
    val deliveryFailReason: String?,
    val rxReturnBagScanTimestamp: ZonedDateTime? = null,
    val rxBagsScanTime: ZonedDateTime? = null,
) : Parcelable, Dto
