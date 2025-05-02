package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
@Parcelize
data class SiteDetailsDto(
    @Json(name = "active") val active: Boolean? = null,
    @Json(name = "address") val address: AddressDto? = null,
    @Json(name = "advancePickingDays") val advancePickingDays: Long? = null,
    // @Json(name = "autoDrop") val autoDrop: ???
    @Json(name = "bagCountRequired") val bagCountRequired: Boolean? = null,
    @Json(name = "bagScanRequired") val bagScanRequired: Boolean? = null,
    @Json(name = "batchOrders") val batchOrders: Boolean? = null,
    @Json(name = "calculateStageBy") val calculateStageBy: Boolean? = null,
    @Json(name = "cancelOrderAllowed") val cancelOrderAllowed: Boolean? = null,
    @Json(name = "concernTime") val concernTime: String? = null,
    @Json(name = "createdBy") val createdBy: String? = null,
    @Json(name = "createdDate") val createdDate: ZonedDateTime? = null,
    @Json(name = "customerArrival") val customerArrival: Boolean? = null,
    // @Json(name = "fulfillmentType") val fulfillmentType: ???
    @Json(name = "handoffDelayTime") val handoffDelayTime: Int? = null,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "imageUrlPart1") val imageUrlPart1: String? = null,
    @Json(name = "imageUrlPart2") val imageUrlPart2: String? = null,
    @Json(name = "isCatalogDataAtRog") val isCatalogDataAtRog: Boolean? = null,
    @Json(name = "isFlashInterjectionEnabled") val isFlashInterjectionEnabled: Boolean? = null,
    @Json(name = "isFlashOrderEnabled") val isFlashOrderEnabled: Boolean? = null,
    @Json(name = "isMultipleHandoffAllowed") val isMultipleHandoffAllowed: Boolean? = null,
    @Json(name = "issueScanning") val issueScanning: Boolean? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "lastModifiedBy") val lastModifiedBy: String? = null,
    @Json(name = "lastModifiedDate") val lastModifiedDate: ZonedDateTime? = null,
    @Json(name = "limitSearchResult") val limitSearchResult: Long? = null,
    @Json(name = "maxOrder") val maxOrder: Long? = null,
    @Json(name = "maxQty") val maxQty: Long? = null,
    @Json(name = "mfc") val mfc: Boolean? = null,
    @Json(name = "mtoAllowed") val mtoAllowed: Boolean? = null,
    // @Json(name = "ordersToBatch") val ordersToBatch: ???
    @Json(name = "parentOrgId") val parentOrgId: String? = null,
    @Json(name = "processData") val processData: Boolean? = null,
    @Json(name = "relationshipType") val relationshipType: RelationshipType? = null,
    @Json(name = "reshop") val reshop: Boolean? = null,
    @Json(name = "rogCode") val rogCode: String? = null,
    @Json(name = "siteId") val siteId: String? = null,
    @Json(name = "siteName") val siteName: String? = null,
    @Json(name = "siteType") val siteType: SiteType? = null,
    @Json(name = "stageByTimeInMinutes") val stageByTimeInMinutes: Long? = null,
    @Json(name = "warningTime") val warningTime: String? = null,
    @Json(name = "stagingType") val stagingType: StagingType? = null,
    @Json(name = "digitizeAgeVerificationEnabled") val isDigitizeAgeVerificationEnabled: Boolean? = null,
    @Json(name = "customerApprovedSubstitutionEnabled") val isCustomerApprovedSubstitutionEnabled: Boolean? = null,
    @Json(name = "isFixedItemType") val fixedItemTypesEnabled: Boolean? = null,
    @Json(name = "isDarkStoreEnabled") val isDarkStoreEnabled: Boolean? = null,
    @Json(name = "isWineFulfillment") val isWineFulfillment: Boolean? = null,
    @Json(name = "isRxDugEnabled") val isRxDugEnabled: Boolean? = null,
    @Json(name = "isMerchantPickEnabled") val partnerPickEnabled: Boolean? = null,
    @Json(name = "isAgeVerificationCameraEnabled") val isAgeVerificationCameraEnabled: Boolean? = null,
    @Json(name = "isCctEnabled") val isCctEnabled: Boolean? = null,
    @Json(name = "digitizeAgeFlags") val digitizeAgeFlags: DigitizedAgeFlags? = null,
    @Json(name = "dugInterjectionEnabled") val dugInterjectionEnabled: Boolean? = null,
    @Json(name = "notificationEtaTime") val notificationEtaTime: NotificationEtaTime? = null,
    // @Json(name = "secondNotificationETATime") val secondNotificationETATime: Int? = null,
    @Json(name = "isEtaArrivalEnabled") val isEtaArrivalEnabled: Boolean? = null,
    @Json(name = "locationFullEnabled") val islocationFullEnabled: Boolean? = null,
    @Json(name = "twoWayCommsFlags") val twoWayCommsFlags: TwoWayCommsFlags? = null,
    @Json(name = "isCas1PL") val isCas1PL: Boolean? = null,
    @Json(name = "weightedItemLimit") val weightedItemLimit: Double? = null,

    @Json(name = "realTimeSubDelay") val realTimeSubDelay: Int? = 0,
    @Json(name = "storeLevelTempFlags") val storeLevelTempFlags: StoreLevelTempFlags? = null,
    @Json(name = "prePickFeatureFlag") val prePickFeatureFlag: PrePickFeatureFlag? = null,
    @Json(name = "prepNotReadyDelayTime") val prepNotReadyDelayTime: Int? = 0
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class DigitizedAgeFlags(
    @Json(name = "dob") val dob: Boolean? = false,
    @Json(name = "id") val id: Boolean? = false,
    @Json(name = "signature") val signature: Boolean? = false
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class TwoWayCommsFlags(
    @Json(name = "realTimeSubstitutions") val realTimeSubstitutions: Boolean? = false,
    @Json(name = "chatBeta") val chatBeta: Boolean? = false,
    @Json(name = "interactiveChat") val enrichedChat: Boolean? = false,
    @Json(name = "voiceToText") val voiceToText: Boolean? = false,
    @Json(name = "chatRetryDelaySecond") val chatRetryDelaySecond: Long? = null,
    @Json(name = "chatRetryTimeIntervalSeconds") val chatRetryTimeIntervalSeconds: Long? = null,
    @Json(name = "chatMaxRetriesWithinInterval") val chatMaxRetriesWithinInterval: Int? = null,
    @Json(name = "chatTotalRetryIntervals") val chatTotalRetryIntervals: Int? = null,
    @Json(name = "masterOrderView") val masterOrderView: Boolean? = false,
    @Json(name = "masterOrderView2") val masterOrderView2: Boolean? = false,
    @Json(name = "customerTyping") val customerTyping: Boolean? = false,
    @Json(name = "rePickOriginalAllowSubForApprdRejd") val allowRepickOriginalItem: Boolean? = false,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class NotificationEtaTime(
    @Json(name = "firstNotificationETATime") val firstNotificationETATime: Int? = null,
    @Json(name = "secondNotificationETATime") val secondNotificationETATime: Int? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class StoreLevelTempFlags(
    @Json(name = "MISSING_LOCATION_ENABLED_APP") val missingLocationEnabledApp: String? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = true)
@Parcelize
data class PrePickFeatureFlag(
    @Json(name = "ALLOW_SUBSTITUTION") val allowSubstitution: String? = null,
    @Json(name = "AUTO_INITIATE") val autoInitiate: String? = null,
    @Json(name = "B2B_AUTO_INITIATE") val b2BAutoInitiate: String? = null,
) : Parcelable, Dto

@JsonClass(generateAdapter = false)
enum class RelationshipType {
    @Json(name = "INTERNAL")
    INTERNAL,

    @Json(name = "VENDOR")
    VENDOR,

    @Json(name = "THREE_PL")
    THREE_PL,
}

@JsonClass(generateAdapter = false)
enum class SiteType {
    @Json(name = "DELIVERY_CENTER")
    DELIVERY_CENTER,

    @Json(name = "FULFILLMENT_CENTER")
    FULFILLMENT_CENTER,

    @Json(name = "MANUFACTURING")
    MANUFACTURING,

    @Json(name = "FC")
    FC,

    @Json(name = "X_DOCK")
    X_DOCK,

    @Json(name = "STORE")
    STORE,
}

@JsonClass(generateAdapter = false)
enum class StagingType {
    @Json(name = "SHOP_FLOOR")
    SHOP_FLOOR,

    @Json(name = "TEMP_ZONE")
    TEMP_ZONE
}
