package com.albertsons.acupick.ui.staging

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.ui.models.ToteUI
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class UnAssignToteParams(
    val stagingActivityId: Long,
    val toteList: List<ToteUI>,
    val customerOrderNumber: String?,
    val activityNo: String?,
    val customerName: String?,
    val shortOrderId: String?,
    val isMultiSource: Boolean?
) : Parcelable
