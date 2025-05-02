package com.albertsons.acupick.ui.staging

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.ui.models.ToteUI
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep // needed due to inclusion in the nav graph - see https://developer.android.com/guide/navigation/navigation-pass-data#proguard_considerations
data class AddBagsUiData(
    val stagingId: Long,
    val toteList: List<ToteUI>,
    val customerOrderNumber: String?,
    val shortOrderId: String?,
    val customeName: String?,
    val isCustomerPreferBag: Boolean
) : Parcelable
