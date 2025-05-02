package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.ContainerType
import com.albertsons.acupick.data.model.StorageType
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
@Keep
data class BagsPerTempZoneParams(
    val name: String,
    val orderNumber: String,
    val orderType: String,
    val startTime: ZonedDateTime?,
    val bagAndLooseItemCount: Int,
    val bagsPerTempZoneDataList: List<BagsPerTempZoneData>?
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
@Keep
data class BagsPerTempZoneData(
    val storageType: StorageType?,
    val containerType: ContainerType?,
    val bags: Int?,
    val loose: Int?,
) : Parcelable
