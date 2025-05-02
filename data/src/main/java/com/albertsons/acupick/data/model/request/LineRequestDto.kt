package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.albertsons.acupick.data.model.SubReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

/**
 * Corresponds to the LineReqWrapper swagger api. For more info, see https://confluence.safeway.com/display/EOM/Record+Pick
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class LineRequestDto(
    @Json(name = "containerId") val containerId: String? = null,
    /**
     * Flag to ignore the validation for existence of containers
     *
     * Pass false for first recordPick api attempt. If response returns [com.albertsons.acupick.data.model.response.ServerErrorCode.ORDER_ALREADY_ASSIGNED],
     * pass true for the 2nd recordPick attempt.
     * Pass true for syncOfflinePicking api calls to prevent sync failures where a scanned tote is associate with another order.
     */
    @Json(name = "disableContainerValidation") val disableContainerValidation: Boolean? = null,
    @Json(name = "fulfilledQty") val fulfilledQty: Double? = null,
    /** ItemActivity db id */
    @Json(name = "iaId") val iaId: Long? = null,
    /** Value should always be set to true in the pick requests (backend validation deprecated - decision is to rely on front end validation) */
    @Json(name = "ignoreUpc") val ignoreUpc: Boolean? = null,
    @Json(name = "isSmartSubItem") val isSmartSubItem: Boolean? = null,
    @Json(name = "modifiedSubstituteUpc") val modifiedSubstituteUpc: String? = null,
    @Json(name = "catalogUpc") val catalogUpc: String? = null,
    @Json(name = "originalItemId") val originalItemId: String? = null,
    @Json(name = "pickedTime") val pickedTime: ZonedDateTime? = null,
    @Json(name = "regulated") val regulated: Boolean? = null,
    @Json(name = "subReasonCode") val subReasonCode: SubReasonCode? = null,
    @Json(name = "substituteItemDesc") val substituteItemDesc: String? = null,
    @Json(name = "substituteItemId") val substituteItemId: String? = null,
    @Json(name = "substitution") val substitution: Boolean? = null,
    @Json(name = "storageType") val storageType: StorageType? = null,
    @Json(name = "upcQty") val upcQty: Double? = null,
    /** UPC code */
    @Json(name = "upcId") val upcId: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "sameItemSubbed") val sameItemSubbed: Boolean? = null,
    @Json(name = "netWeight") val netWeight: Double? = null,
    @Json(name = "isPickCompleted") val isPickCompleted: Boolean? = null,
    @Json(name = "scannedPrice") val scannedPrice: Double? = null,
    @Json(name = "sellByWeightInd") val sellByWeightInd: SellByType? = null,
    @Json(name = "exceptionDetailsId") val exceptionDetailsId: Long? = null,
    @Json(name = "substitutionReason") val substitutionReason: SubstitutionRejectedReason? = null,
    @Json(name = "messageSid") val messageSid: String? = null,
    @Json(name = "isManuallyEntered") val isManuallyEntered: Boolean? = null
) : Parcelable, Dto
