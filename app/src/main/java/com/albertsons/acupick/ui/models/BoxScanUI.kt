package com.albertsons.acupick.ui.models

import com.albertsons.acupick.data.model.BoxData
import com.albertsons.acupick.data.model.ScannedBoxData
import java.time.ZonedDateTime

data class BoxScanUI(
    val box: BoxUI,
    val zone: String,
    val orderNumber: String,
    val containerScanTime: ZonedDateTime? = null,
) : UIModel {

    val asScannedBoxData: ScannedBoxData
        get() {
            return ScannedBoxData(
                box = BoxData(
                    referenceEntityId = box.referenceEntityId,
                    type = box.type,
                    zoneType = box.zoneType,
                    orderNumber = box.orderNumber,
                    boxNumber = box.boxNumber,
                    label = box.label
                ),
                zone = zone,
                orderNumber = orderNumber,
                containerScanTime = containerScanTime,
                isLoose = box.isLoose
            )
        }
}

fun ScannedBoxData.toBoxScanUI(): BoxScanUI {
    return BoxScanUI(
        box = BoxUI(
            zoneType = this.box.zoneType,
            referenceEntityId = this.box.referenceEntityId,
            type = this.box.type,
            orderNumber = this.box.orderNumber,
            boxNumber = this.box.boxNumber,
            isLoose = this.isLoose,
            label = this.box.label
        ),
        zone = this.zone,
        orderNumber = this.orderNumber,
        containerScanTime = this.containerScanTime
    )
}
