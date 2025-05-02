package com.albertsons.acupick.ui.itemdetails

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.ui.picklistitems.PickListType
import kotlinx.parcelize.Parcelize

@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
@Parcelize
data class ItemDetailsParams(
    /** Maps to [com.albertsons.acupick.data.model.response.ItemActivityDto.id] */
    val iaId: Long,
    val actId: Long,
    val activityNo: String,
    val altItemLocations: List<ItemLocationDto>?,
    val pickListType: PickListType,
    val isFromSubstitutionFlow: Boolean = false,
    val isMoveToLocation: Boolean = false
) : Parcelable

@Parcelize
data class UnPickParams(
    /** Maps to [com.albertsons.acupick.data.model.response.ItemActivityDto.id] */
    val iaId: Long,
    val actId: Long,
    val activityNo: String,
    val pickListType: PickListType
) : Parcelable

@Parcelize
data class UnPickResultParams(
    val checkedItems: List<ItemActionBackingType>
) : Parcelable
