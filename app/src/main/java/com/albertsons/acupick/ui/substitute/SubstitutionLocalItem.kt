package com.albertsons.acupick.ui.substitute

import android.os.Parcelable
import com.albertsons.acupick.data.model.barcode.BarcodeType
import com.albertsons.acupick.data.model.response.ItemDetailDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubstitutionLocalItem(
    val item: ItemDetailDto,
    val selectedVariant: BulkItem?,
    val itemBarcodeType: BarcodeType.Item?,
    var toteBarcodeType: BarcodeType?,
    var quantity: Double = 1.0,
    val itemWeight: String?,
    val unitOfMeasure: String?,
    val orderedByWeight: Boolean,
    val isCustomerChosenItemAvailable: Boolean? = false,
    val isIssueScanned: Boolean = false,
    val isDisplayType3Pw: Boolean = false,
    val orderedWeightWithUom: String = ""
) : Parcelable
