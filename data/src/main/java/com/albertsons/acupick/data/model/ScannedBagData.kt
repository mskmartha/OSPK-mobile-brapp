package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class ScannedBagData(
    val bag: BagData,
    val zone: String,
    val customerOrderNumber: String,
    val containerScanTime: ZonedDateTime? = null,
    val isLoose: Boolean,
) : Parcelable, Dto
