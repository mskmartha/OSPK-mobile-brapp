package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.RequestedAmount
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.ui.util.StringIdHelper
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class ConfirmAmountUIData(
    val pageTitle: StringIdHelper,
    val itemDescription: String,
    val requestedAmount: RequestedAmount,
    val itemType: SellByType
) : Parcelable
