package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the ItemAttribute swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemAttributeDto(
    @Json(name = "depId") val depId: String? = null,
    @Json(name = "depName") val depName: String? = null,
    @Json(name = "dimension") val dimensionDto: DimensionDto? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "itemCatLevelOne") val itemCatLevelOne: String? = null,
    @Json(name = "itemDesc") val itemDesc: String? = null,
    @Json(name = "itemStorageType") val itemStorageType: String? = null,
    @Json(name = "itemWeight") val itemWeight: String? = null,
    @Json(name = "itemWeightUom") val itemWeightUom: String? = null,
    @Json(name = "pluCode") val pluCode: String? = null,
    @Json(name = "preprCategory") val prepCategory: String? = null,
    @Json(name = "productDesc") val productDesc: String? = null,
    @Json(name = "productId") val productId: String? = null,
    @Json(name = "sellByWeightInd") val sellByWeightInd: String? = null
) : Parcelable, Dto
