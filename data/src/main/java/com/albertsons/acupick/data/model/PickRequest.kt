package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer

data class PickRequest(
    /**
     * Item bpn id to use in combination with customerOrderNumber as the source of truth when looking up the matching item elsewhere for this pick request.
     *
     * Needed because upc might not be present for an item even though we know from the substitution flow/ itemDetails api call the matching bpn for the upc
     */
    val itemBpnId: String?,
    /**
     * Used in combination with item bpn id as the source of truth when looking up the matching item elsewhere for this pick request.
     */
    val customerOrderNumber: String?,
    val itemBarcodeType: BarcodeType.Item,
    val toteBarcodeType: PickingContainer,
    /**
     * Number of times a UPC is scanned and needs to be billed to customer.
     *
     * For I, W and P items it is the actual physical UPCs and scans. For single scan of a UPC, it should be '1'. If same UPC is scanned twice then it should be '2'
     * For E items it is the actual physical items and should be set to the qty entered by picker in the app.
     *
     * In case of substitution, follow the same logic as above
     */
    val upcQuantity: Double,
    /**
     * Number of items fulfilled for customer
     *
     * For I, E and P items it is the actual physical item qty and same as upcQty
     * For W items it is the actual physical items and the qty as entered by picker. Since in MLP, we don't have the functionality to enter this, please default to original ordered qty for the item.
     * In case of substitution also, follow the same logic as above.
     */
    val fulfilledQuantity: Double,
    val userId: String,
    /**
     * Flag to ignore the validation for existence of containers
     *
     * Pass false for first recordPick api attempt. If response returns [com.albertsons.acupick.data.model.response.ServerErrorCode.ORDER_ALREADY_ASSIGNED],
     * pass true for the 2nd recordPick attempt.
     * Pass true for syncOfflinePicking api calls to prevent sync failures where a scanned tote is associate with another order.
     *
     * See [com.albertsons.acupick.data.model.request.LineRequestDto.disableContainerValidation]
     */
    val disableContainerValidation: Boolean,
    val netWeight: Double?,
    val scannedPrice: Double?,
    val sellByWeightInd: SellByType?,
    val storageType: StorageType?,
    val isManuallyEntered: Boolean
) : DomainModel
