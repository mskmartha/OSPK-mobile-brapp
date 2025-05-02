package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class ScannedBoxData(
    val box: BoxData,
    val zone: String,
    val orderNumber: String,
    val containerScanTime: ZonedDateTime? = null,
    val isLoose: Boolean
) : Parcelable, Dto
