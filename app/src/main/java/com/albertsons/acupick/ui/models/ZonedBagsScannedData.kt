package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

// Todo - this needs to be cleaned up, it is no longer a group of bags but instead a single bags data
@Parcelize
@Keep
data class ZonedBagsScannedData(
    val bagData: BagLabel?,
    var currentBagsScanned: Int = 0,
    var bagsScanned: Int = 0,
    var looseScanned: Int = 0,
    var totalBagsForZone: Int = 1,
    var totalBags: Int = 1,
    var totalLoose: Int = 1,
    var totalBagsPerLocation: Int = 0,
    var totalLoosePerLocation: Int = 0,
    var isActive: Boolean = false,
    var bagsForcedScanned: Int = 0,
    var looseItemCount: Int = 0,
) : Parcelable {
    constructor(bagLabel: BagLabel) : this(
        bagData = bagLabel
    )

    fun isComplete() = currentBagsScanned + bagsForcedScanned >= totalBagsForZone

    fun forceScanBags() {
        bagsForcedScanned = totalBagsForZone - currentBagsScanned
    }
}

fun List<ZonedBagsScannedData>.takeOrder(customerOrderNumber: String?) = this.filter { it.bagData?.customerOrderNumber == customerOrderNumber }
