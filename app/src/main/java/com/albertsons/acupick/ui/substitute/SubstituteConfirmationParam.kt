package com.albertsons.acupick.ui.substitute

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.SellByType
import kotlinx.parcelize.Parcelize

@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerationsclass
@Parcelize
data class SubstituteConfirmationParam(
    val substituteItemList: List<SubstitutionLocalItem>,
    val imageUrl: String?,
    val description: String?,
    val requestedCount: String,
    val isOrderedByWeight: Boolean,
    val isDisplayType3PW: Boolean,
    val requestedWeightAndUnits: String?,
    val hadIssueScanning: Boolean = false,
    val sellByType: SellByType? = SellByType.RegularItem,
    val isBulk: Boolean = false,
    val iaId: Long? = null,
    val siteId: String? = "",
    val messageSid: String? = null,
    val isCustomerBagPreference: Boolean? = null
) : Parcelable

@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerationsclass
@Parcelize
data class BulkSubstituteConfirmationParam(
    val bulkItems: List<BulkItem>,
) : Parcelable
