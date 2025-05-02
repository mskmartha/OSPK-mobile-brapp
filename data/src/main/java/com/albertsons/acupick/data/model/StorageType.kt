package com.albertsons.acupick.data.model

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
enum class StorageType(val zonePrefix: String) {
    /** ambient */
    @Json(name = "AM") AM("AM"),
    /** chilled */
    @Json(name = "CH") CH("CH"),
    /** frozen */
    @Json(name = "FZ") FZ("FZ"),
    /** hot **/
    @Json(name = "HT") HT("HOT"),
}
