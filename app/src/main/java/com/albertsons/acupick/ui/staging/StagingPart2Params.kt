package com.albertsons.acupick.ui.staging

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.ui.models.ToteUI
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class StagingPart2Params(
    val pickingActivityId: Long,
    val stagingActivityId: Long,
    val toteList: List<ToteUI>,
    val isPrintingStillNeeded: Boolean,
    val customerOrderNumber: String?,
) : Parcelable
