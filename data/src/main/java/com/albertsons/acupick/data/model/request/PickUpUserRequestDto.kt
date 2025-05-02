package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.PickupType
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class PickUpUserRequestDto(
    val orderNumber: String,
    val pickupPerson: PickupPersonDto,
    val pickupTimeStamp: ZonedDateTime,
    val siteId: String,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class PickupPersonDto(
    val pickupPersonDOB: String,
    val pickupPersonName: String,
    val pickupPersonType: PickupType,
    val pickupPersonIDType: String?,
    val pickupPersonIDNumber: String?,
    val pickupPersonSignature: String?,
) : Parcelable
