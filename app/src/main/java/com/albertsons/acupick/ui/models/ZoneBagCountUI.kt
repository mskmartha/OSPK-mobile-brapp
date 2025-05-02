package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.StorageType
import kotlinx.android.parcel.Parcelize

/** Represent a row in the zone type cards */
@Parcelize
@Keep
data class ZoneBagCountUI(
    /** the name of the zone (e.g., AM01, CH02) */
    val zone: String,
    /** the temperature zone type (e.g., ambient, chilled, or frozen) */
    val zoneType: StorageType?,
    var scannedBagCount: Int = 0,
    // TODO - look into removing this after refactor
    /** whether it is the currently active zone */
    var isCurrent: Boolean = true,
    val isMultiSource: Boolean?,
    val bagOrToteScannedCount: Int = 0,
    val looseScannedCount: Int = 0,
    val newlocation: Boolean = false
) : Parcelable
