package com.albertsons.acupick.data.model

import android.os.Parcelable
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemActivityDto
import com.albertsons.acupick.data.model.response.ItemDetailDto
import kotlinx.android.parcel.Parcelize

/**
 * Combination of barcode scan with the actual item to uniquely represent an item. Note that for substitutions, [item] represents the original item the picker is substituting while the [barcodeType]
 * represents the substitute barcode.
 *
 * A necessary grouping to uniquely identify items now that batch orders can have identical items
 * with different customer order numbers. Uniqueness can no longer be determined by just a barcode but also needs
 * to take the customer order number (or [ItemActivityDto.id]) into account.
 */
@Parcelize
data class ScannedPickItem(
    val barcodeType: BarcodeType.Item,
    val item: ItemActivityDto,
    val itemDetails: ItemDetailDto? = null,
) : Parcelable
