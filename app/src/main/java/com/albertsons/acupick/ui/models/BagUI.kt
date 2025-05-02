package com.albertsons.acupick.ui.models

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.StorageType
import kotlinx.android.parcel.Parcelize

/** represents a bag or a loose item */
@Parcelize
@Keep
data class BagUI(
    val bagId: String,
    val zoneType: StorageType,
    val customerOrderNumber: String?,
    val fulfillmentOrderNumber: String?,
    val contactFirstName: String?,
    val contactLastName: String?,
    val containerId: String,
    val isBatch: Boolean,
    val isLoose: Boolean,
) : UIModel, Parcelable

fun BagUI.fullContactName() = "${contactFirstName.orEmpty()} ${contactLastName.orEmpty()}".trim()
