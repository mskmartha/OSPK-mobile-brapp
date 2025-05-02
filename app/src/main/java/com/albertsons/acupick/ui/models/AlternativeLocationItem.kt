package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.io.Serializable

data class AlternativeLocationItem(
    val itemName: String?,
    val upc: String?,
    val imageUrl: String?,
    val alternativeLocations: List<String>,
    val path: AlternateLocationPath,
) : Serializable

@Parcelize
@Keep
enum class AlternateLocationPath : Parcelable {
    /** This item is in the short flow */
    Short,
    /** This item is in the substitute flow */
    Substitute,
}
