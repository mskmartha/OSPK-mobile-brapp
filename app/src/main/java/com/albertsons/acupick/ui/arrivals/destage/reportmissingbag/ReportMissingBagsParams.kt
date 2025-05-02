package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.ui.models.ZonedBagsScannedData
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class ReportMissingBagsParams(
    val zoneDataList: List<ZonedBagsScannedData>?,
    val isMissingBags: Boolean?,
    val storageType: StorageType?,
    val currentPage: Int,
    val isMfcSite: Boolean?,
    val isCustomerBagPreference: Boolean?,
    val isLooseItemLableMissing: Boolean = false,
    val isLooseItemMissing: Boolean = false,
) : Parcelable
