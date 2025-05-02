package com.albertsons.acupick.ui.models

import android.os.Parcelable
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MissingItemLocationParams(
    val itemDescription: String,
    val itemImage: String?,
    val itemUpcId: String,
    val itemLocation: String? = null,
    val scannedData: PickListScannedData? = null
) : Parcelable

@Parcelize
data class PickListScannedData(
    val item: ItemActivityDto,
    val scannedBarcodeResult: BarcodeType? = null
) : Parcelable
