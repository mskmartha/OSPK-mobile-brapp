package com.albertsons.acupick.ui.models

import com.albertsons.acupick.data.model.BagData
import com.albertsons.acupick.data.model.ScanContainerReasonCode
import com.albertsons.acupick.data.model.ScannedBagData
import java.time.ZonedDateTime

/** represents a scanned bag or loose item */
data class BagScanUI(
    val bag: BagUI,
    val zone: String,
    val customerOrderNumber: String,
    val containerScanTime: ZonedDateTime? = null,
    val scanContainerReasonCode: ScanContainerReasonCode? = null
) : UIModel {

    val asScannedBagData: ScannedBagData
        get() {
            return ScannedBagData(
                bag = BagData(
                    bagId = bag.bagId,
                    zoneType = bag.zoneType,
                    customerOrderNumber = bag.customerOrderNumber,
                    fulfillmentOrderNumber = bag.fulfillmentOrderNumber,
                    contactFirstName = bag.contactFirstName,
                    contactLastName = bag.contactLastName,
                    containerId = bag.containerId,
                    isBatch = bag.isBatch
                ),
                zone = zone,
                customerOrderNumber = customerOrderNumber,
                containerScanTime = containerScanTime,
                isLoose = bag.isLoose
            )
        }

    companion object {
        fun fromScannedBagData(scannedBagData: ScannedBagData): BagScanUI {
            return BagScanUI(
                bag = BagUI(
                    bagId = scannedBagData.bag.bagId,
                    zoneType = scannedBagData.bag.zoneType,
                    customerOrderNumber = scannedBagData.bag.customerOrderNumber,
                    fulfillmentOrderNumber = scannedBagData.bag.fulfillmentOrderNumber,
                    contactFirstName = scannedBagData.bag.contactFirstName,
                    contactLastName = scannedBagData.bag.contactLastName,
                    containerId = scannedBagData.bag.containerId,
                    isBatch = scannedBagData.bag.isBatch,
                    isLoose = scannedBagData.isLoose,
                ),
                zone = scannedBagData.zone,
                customerOrderNumber = scannedBagData.customerOrderNumber,
                containerScanTime = scannedBagData.containerScanTime,
            )
        }
    }
}
