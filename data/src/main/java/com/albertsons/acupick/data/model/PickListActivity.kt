package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.albertsons.acupick.data.model.response.ItemActivityDto
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PickListActivity(
    val actId: Long? = null,
    val customerOrderNumber: String? = null,
    val itemActivitiesMap: Map<String, List<ItemActivityDto>?> = emptyMap(),
    val listOfOrderNumber: List<String>? = null,
) : Parcelable, DomainModel

val PickListActivity.itemActivities get() = itemActivitiesMap.values.flatMap { it.orEmpty() }
