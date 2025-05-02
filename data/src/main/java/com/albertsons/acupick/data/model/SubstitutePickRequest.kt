package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.barcode.PickingContainer
import com.albertsons.acupick.data.model.request.SubstitutionRejectedReason
import com.albertsons.acupick.data.model.response.ItemActivityDto

data class SubstitutePickRequest(
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
    val originalItem: ItemActivityDto,
    val substituteItem: SubstitutedItem,
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
    val isSmartSubItem: Boolean,
    val subReasonCode: SubReasonCode?,
    val regulated: Boolean?,
    /**
     * Flag for when an item BPN is substituted by the same item BPN
     */
    val sameItemSubbed: Boolean,
    val scannedPrice: Double?,
    val exceptionDetailsId: Long? = null,
    /**
     * Flag substitutionReason to identify that the substitution was performed as part of swap substitution flow
     */
    val substitutionReason: SubstitutionRejectedReason? = null,
    val sellByWeightInd: SellByType? = null,
    val messageSid: String? = null,
    val isManuallyEntered: Boolean
) : DomainModel
