package com.albertsons.acupick.data.model.request

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

@JsonClass(generateAdapter = true)
@Parcelize
data class ConfigFlagRequest(
    @Json(name = "appCode") val appCode: String? = APP_CODE,
    @Json(name = "version") val version: String? = VERSION,
    @Json(name = "attributes") val attributes: FeatureFlagAttributeDto? = null
) : Parcelable, Dto {
    companion object {
        const val APP_CODE = "ospkapp"
        const val VERSION = "1.0"
    }
}
