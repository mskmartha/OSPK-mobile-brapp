package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.EndPickReasonCode
import com.albertsons.acupick.data.model.ShortReasonCode
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the PickCompleteReq swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class PickCompleteRequestDto(
    @Json(name = "actId") val actId: Long? = null,
    @Json(name = "autoShortage") val autoShortage: Boolean? = null,
    @Json(name = "shortageReasonCode") val shortageReasonCode: ShortReasonCode? = null, // TODO: End-Pick reason code old attribute will remove once confirm from BE
    @Json(name = "endPickReasonCode") val endPickReasonCode: EndPickReasonCode? = null,
    @Json(name = "skipCompleteValidation") val skipCompleteValidation: Boolean? = null,
    @Json(name = "userId") val userId: String? = null,
) : Parcelable, Dto
