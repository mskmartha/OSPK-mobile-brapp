package com.albertsons.acupick.ui.manualentry

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.BagUI
import com.albertsons.acupick.ui.models.BoxUI
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import kotlinx.parcelize.Parcelize

// TODO: Consider an approach that clearly delineates which params are used for entry type rather than a single data class consisting of all possible arguments/properties
@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class ManualEntryPickParams(
    val isSubstitution: Boolean,
    val selectedItem: ItemActivityDto? = null,
    val requestedQty: Int? = null,
    val remainingRequestedQty: Int? = null,
    val stageByTime: String? = null,
    val substitutedCount: Int? = null,
    val entryType: ManualEntryType?,
    val isIssueScanning: Boolean = false,
    val isBulk: Boolean = false,
) : Parcelable

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class ManualEntryStagingParams(
    val scannedBagUiList: List<BagUI>? = null,
    val scannedBoxUiList: List<BoxUI>? = null,
    val zone: String? = null,
    val isWineShipping: Boolean = false,
    val customerOrderNumber: String? = null,
    val activityId: String? = null,
    val isMutliSource: Boolean? = null,
    val shortOrderId: String? = null,
    val customerName: String? = null,
    val isCustomerPreferBag: Boolean = true
) : Parcelable

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class ManualEntryHandoffParams(
    val bagLabels: ArrayList<BagLabel>? = null,
    val customerOrderNumber: String? = null,
    val activityId: String?,
    val isMutliSource: Boolean? = null,
    val shortOrderId: String? = null,
    val customerName: String? = null,
) : Parcelable

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class ManualEntryPharmacyParams(
    val orderNumber: String,
    val scanTarget: ScanTarget,
    val shortOrderId: String? = null,
    val customerName: String? = null,
    val customerOrderNumber: String?
) : Parcelable
