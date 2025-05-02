package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.albertsons.acupick.data.model.SellByType
import com.albertsons.acupick.data.model.StorageType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent

/**
 * Corresponds to the ItemDetail swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class ItemDetailDto(
    @Json(name = "imageURL") val imageUrl: String?,
    @Json(name = "itemDesc") val itemDesc: String?,
    @Json(name = "itemId") val itemId: String?,
    @Json(name = "storageType") val storageType: StorageType?,
    @Json(name = "isRegulated") val isRegulated: Boolean?,
    @Json(name = "displayType") val displayType: Long?,
    @Json(name = "sellByWeightInd") val sellByWeightInd: SellByType?,
    @Json(name = "basePrice") val basePrice: Double? = null,
    @Json(name = "basePricePer") val basePricePer: Double? = null,
    @Json(name = "itemWeight") val itemWeight: String? = null,
    @Json(name = "pluList") val pluList: List<String?>? = null,
    @Json(name = "exceptionDetailsId") val exceptionDetailsId: Long? = null,
    @Json(name = "bulkVariantList") val bulkVariantList: List<BulkItemsDto?>? = null,
    @Transient val bulkVariantMap: Map<String, List<BulkItemsDto?>?>? = null,
) : Parcelable, Dto, KoinComponent {
    fun isOrderedByWeight() = sellByWeightInd == SellByType.Weight &&
        displayType == ItemActivityDto.DISPLAY_TYPE_ORDERED_BY_WEIGHT

    fun isDisplayType3PW() = sellByWeightInd == SellByType.PriceWeighted &&
        displayType == ItemActivityDto.DISPLAY_TYPE_ORDERED_BY_WEIGHT

    constructor(itemActivityDto: ItemActivityDto) : this(
        imageUrl = itemActivityDto.imageUrl,
        itemDesc = itemActivityDto.itemDescription,
        itemId = itemActivityDto.itemId,
        storageType = itemActivityDto.storageType,
        isRegulated = null,
        displayType = itemActivityDto.displayType,
        sellByWeightInd = itemActivityDto.sellByWeightInd,
        bulkVariantList = itemActivityDto.bulkVariantList,
        bulkVariantMap = itemActivityDto.bulkVariantMap
    )

    companion object {

        val unknownItem: ItemDetailDto by lazy {
            ItemDetailDto(
                imageUrl = null,
                itemDesc = "No description available",
                itemId = "",
                storageType = null,
                displayType = null,
                isRegulated = null,
                sellByWeightInd = null,
            )
        }
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class BulkItemsDto(
    val itemDesc: String?,
    val imageURL: String?,
    val itemId: String?,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class Bulk(val bulkItemsMap: Map<String, BulkItemsDto>) : Parcelable
