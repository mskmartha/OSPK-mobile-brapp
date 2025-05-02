package com.albertsons.acupick.ui.manualentry

import android.os.Parcelable
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.ui.manualentry.handoff.MAX_ENTRY_LENGTH_ZONES
import com.albertsons.acupick.ui.manualentry.pick.MAX_ENTRY_LENGTH_PLU
import com.albertsons.acupick.ui.manualentry.pick.MAX_ENTRY_LENGTH_UPC
import com.albertsons.acupick.ui.models.BagLabel
import com.albertsons.acupick.ui.models.BagUI
import com.albertsons.acupick.ui.models.BoxUI
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import kotlinx.parcelize.Parcelize

@Parcelize
data class ManualEntryUpcUi(
    val maxEntryLength: Int,
    val isSubstitution: Boolean,
    val substitutedCount: Int?,
    val selectedItem: ItemActivityDto?,
    val stageByTime: String? = null,
    val requestedQty: Int? = null,
    val entryType: ManualEntryType? = null,
    val isIssueScanning: Boolean = false
) : Parcelable {
    constructor(manualEntryParams: ManualEntryPickParams) : this(
        selectedItem = manualEntryParams.selectedItem,
        maxEntryLength = MAX_ENTRY_LENGTH_UPC,
        isSubstitution = manualEntryParams.isSubstitution,
        substitutedCount = manualEntryParams.substitutedCount,
        stageByTime = manualEntryParams.stageByTime,
        requestedQty = manualEntryParams.requestedQty,
        entryType = manualEntryParams.entryType,
        isIssueScanning = manualEntryParams.isIssueScanning
    )
}

@Parcelize
data class ManualEntryPluUi(
    val maxEntryLength: Int = MAX_ENTRY_LENGTH_PLU,
    val isSubstitution: Boolean,
    val substitutedCount: Int?,
    val selectedItem: ItemActivityDto?,
    val stageByTime: String? = null,
    val defaultValue: String? = "",
    val requestedQty: Int? = null,
    val isIssueScanning: Boolean = false,
) : Parcelable {

    constructor(manualEntryParams: ManualEntryPickParams) : this(
        selectedItem = manualEntryParams.selectedItem,
        isSubstitution = manualEntryParams.isSubstitution,
        substitutedCount = manualEntryParams.substitutedCount,
        stageByTime = manualEntryParams.stageByTime,
        defaultValue = if (manualEntryParams.isBulk || !manualEntryParams.isSubstitution) manualEntryParams.selectedItem?.pluList?.getOrNull(0).orEmpty() else "",
        requestedQty = manualEntryParams.requestedQty,
        isIssueScanning = manualEntryParams.isIssueScanning
    )
}

@Parcelize
data class ManualEntryWeightUi(
    val maxEntryLength: Int = MAX_ENTRY_LENGTH_PLU,
    val isSubstitution: Boolean,
    val substitutedCount: Int?,
    val selectedItem: ItemActivityDto?,
    val stageByTime: String? = null,
    val requestedQty: Int? = null,
    val isIssueScanning: Boolean = false,
    val isFromPicking: Boolean = false
) : Parcelable {
    constructor(manualEntryParams: ManualEntryPickParams) : this(
        selectedItem = manualEntryParams.selectedItem,
        isSubstitution = manualEntryParams.isSubstitution,
        substitutedCount = manualEntryParams.substitutedCount,
        stageByTime = manualEntryParams.stageByTime,
        requestedQty = manualEntryParams.requestedQty,
        isIssueScanning = manualEntryParams.isIssueScanning,
        isFromPicking = manualEntryParams.isSubstitution.not() && manualEntryParams.isIssueScanning.not() // To validate max weight limit only applicable in picking flow
    )
}

@Parcelize
data class ManualEntryStagingUi(
    val scannedBagUiList: List<BagUI>? = null,
    val boxList: List<BoxUI>? = null,
    val maxEntryLength: Int,
    val defaultValue: String? = "",
    val customerOrderNumber: String? = "",
    val activityId: String? = "",
    val isWineOrder: Boolean,
    val isMutliSource: Boolean? = null,
    val shortOrderId: String? = null,
    val customerName: String? = null,
    val isCustomerPreferBag: Boolean = true
) : Parcelable {
    constructor(manualEntryParams: ManualEntryStagingParams) : this(
        scannedBagUiList = manualEntryParams.scannedBagUiList,
        boxList = manualEntryParams.scannedBoxUiList,
        maxEntryLength = MAX_ENTRY_LENGTH_ZONES,
        defaultValue = manualEntryParams.zone,
        isWineOrder = manualEntryParams.isWineShipping,
        customerOrderNumber = manualEntryParams.customerOrderNumber,
        activityId = manualEntryParams.activityId,
        isMutliSource = manualEntryParams.isMutliSource,
        shortOrderId = manualEntryParams.shortOrderId,
        customerName = manualEntryParams.customerName,
        isCustomerPreferBag = manualEntryParams.isCustomerPreferBag
    )
}

@Parcelize
data class ManualEntryHandOffUi(
    val bagLabels: List<BagLabel>?,
    val customerOrderNumber: String? = "",
    val activityId: String? = "",
    val isMutliSource: Boolean? = null,
    val shortOrderId: String? = null,
    val customerName: String? = null
) : Parcelable {
    constructor(manualEntryParams: ManualEntryHandoffParams) : this(
        bagLabels = manualEntryParams.bagLabels,
        customerOrderNumber = manualEntryParams.customerOrderNumber,
        activityId = manualEntryParams.activityId,
        isMutliSource = manualEntryParams.isMutliSource,
        shortOrderId = manualEntryParams.shortOrderId,
        customerName = manualEntryParams.customerName
    )
}

@Parcelize
data class ManualEntryPharmacyUi(
    val orderNumber: String,
    val scanTarget: ScanTarget,
    val shortOrderId: String? = null,
    val customerName: String? = null,
    val customerOrderNumber: String?
) : Parcelable {
    constructor(manualEntryParams: ManualEntryPharmacyParams) : this(
        orderNumber = manualEntryParams.orderNumber,
        scanTarget = manualEntryParams.scanTarget,
        shortOrderId = manualEntryParams.shortOrderId,
        customerName = manualEntryParams.customerName,
        customerOrderNumber = manualEntryParams.customerOrderNumber
    )
}
