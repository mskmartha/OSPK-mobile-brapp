package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.CustomerArrivalStatus
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class ScanContDto(
    @Json(name = "scanContActDto") val scanContActDto: List<ScanContActDto>,
    @Json(name = "subStatus") val subStatus: CustomerArrivalStatus? = null,
    @Json(name = "nextActExpStartTime") val nextActExpStartTime: ZonedDateTime? = null,
    @Json(name = "vehicleInfo") val vehicleInfo: VehicleInfoDto? = null,
    @Json(name = "feScreenStatus") val feScreenStatus: String? = null
) : Parcelable, Dto
