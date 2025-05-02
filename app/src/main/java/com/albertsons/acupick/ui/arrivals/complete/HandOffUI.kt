package com.albertsons.acupick.ui.arrivals.complete

import android.content.Context
import android.os.Parcelable
import android.telephony.PhoneNumberUtils
import androidx.annotation.Keep
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.ActivityStatus
import com.albertsons.acupick.data.model.CartType
import com.albertsons.acupick.data.model.EntityReference
import com.albertsons.acupick.data.model.FulfillmentAttributeDto
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.IssuesScanningBag
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.request.firstInitialDotLastName
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.OrderStatus
import com.albertsons.acupick.data.model.response.OrderSummary
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.VehicleColour
import com.albertsons.acupick.data.model.response.VehicleInfoDto
import com.albertsons.acupick.data.model.response.VehicleType
import com.albertsons.acupick.data.model.response.asFirstInitialDotLastString
import com.albertsons.acupick.data.model.response.fullContactName
import com.albertsons.acupick.data.model.response.getFulfillmentTypeDescriptions
import com.albertsons.acupick.data.model.response.isRxDug
import com.albertsons.acupick.data.model.response.isSubstitutionOrIssueScanning
import com.albertsons.acupick.data.model.response.toOrderSummary
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.infrastructure.utils.orZero
import com.albertsons.acupick.ui.models.CustomerInfo
import com.albertsons.acupick.ui.models.CustomerArrivalStatusUI
import com.albertsons.acupick.ui.models.convertArrivalStatus
import com.albertsons.acupick.ui.util.fullDriverName
import com.albertsons.acupick.ui.util.notZeroOrNull
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.orTrue
import kotlinx.parcelize.Parcelize
import org.koin.java.KoinJavaComponent.inject
import java.time.ZonedDateTime
import java.util.Locale

@Parcelize
@Keep
data class HandOffUI(
    val scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto?,
    val confirmRxPickupRequestDto: ConfirmRxPickupRequestDto?,
    val items: List<HandOffRegulatedItem>,
    var name: String,
    val nameAbbreviated: String,
    val tabLabel: String,
    val activityId: Long,
    val erId: Long,
    val siteId: String,
    val orderNumber: String,
    val orderType: String,
    val cartType: CartType?,
    val vehicleDescription: String,
    val location: String,
    val assignedTo: String,
    val confirmOrderText: String,
    val startTime: ZonedDateTime?,
    val isDugOrder: Boolean,
    val isRxDug: Boolean,
    val isPharmacyServicingOrders: Boolean?,
    val isAuthDugEnabled: Boolean?,
    val isDeliveryOrder: Boolean,
    val provider: String,
    val driverName: String?,
    val isHandOffPreComplete: Boolean,
    val confirmOrderTime: ZonedDateTime?,
    val customerArrivalStatusUI: CustomerArrivalStatusUI?,
    val issueScanningBags: List<IssuesScanningBag>?,
    val authenticatedPin: String?,
    val isRegulated: Boolean?,
    val minimumAgeRequired: Int?,
    val fulfillmentType: FulfillmentAttributeDto?,
    val handshakeRequired: Boolean,
    val toteCount: Int,
    val bagCount: Int,
    val looseItemCount: Int,
    val entityReference: EntityReference?,
    val rejectedItems: List<RemoveItemsRequestDto>,
    val customerInfoData: CustomerInfo?,
    val createdOrders: String?,
    val bagsPerTempZoneParams: BagsPerTempZoneParams?,
    val scheduledPickupTimestamp: ZonedDateTime?,
    val rxOrderIds: List<String>?,
    val rxOrderStatus: OrderStatus?,
    val status: ActivityStatus?,
    val groceryDestageCompleteTimestamp: ZonedDateTime?,
    val groceryDestageStartTimestamp: ZonedDateTime?,
    val rxDeliveryFailedReason: String?,
    val orderSummary: List<OrderSummary>?,
    val isCas: Boolean?,
    val is3p: Boolean?,
    val source: String,
    val isPartnerPickDug: Boolean,
    val driverInfoHeader: Int,
    val phoneNumber: String?,
    val spotNumber: String?,
    val vehicleInformation: String?,
    val vehicleLocation: String?,
    val vehicleImageInfo: Pair<Int, Int>?,
    val deliveryInstruction: String?,
    val feScreenStatus: String?,
    val isCustomerBagPreference: Boolean,
    val isGiftLabelPrinted: Boolean?
) : Parcelable {

    constructor(
        activityDto: ActivityDto,
        scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto?,
        confirmRxPickupRequestDto: ConfirmRxPickupRequestDto?,
        issueScanningBags: List<IssuesScanningBag>? = null,
        rejectedItems: List<RemoveItemsRequestDto> = listOf(),
        customerInfoData: CustomerInfo? = null,
        bagsPerTempZoneParams: BagsPerTempZoneParams? = null,
        groceryDestageStartTimestamp: ZonedDateTime?,
        groceryDestageCompleteTimestamp: ZonedDateTime?,
        rxDeliveryFailedReason: String? = null,
        updatedVehicleInfo: ScanContDto? = null,
        confirmOrderTime: ZonedDateTime? = null,
        isGiftLabelPrinted: Map<String, Boolean> = emptyMap()
    ) : this(
        scanContainerWrapperRequestDto = scanContainerWrapperRequestDto,
        confirmRxPickupRequestDto = confirmRxPickupRequestDto,
        items = regulatedItemsList(activityDto),
        name = activityDto.fullContactName(),
        nameAbbreviated = activityDto.asFirstInitialDotLastString(),
        tabLabel = activityDto.asFirstInitialDotLastString(),
        activityId = activityDto.actId ?: 0,
        erId = activityDto.erId ?: 0,
        orderNumber = activityDto.customerOrderNumber ?: "",
        siteId = activityDto.siteId ?: "",
        orderType = activityDto.getFulfillmentTypeDescriptions(),
        vehicleDescription = activityDto.additionalActData?.vehicleDetail ?: "",
        location = activityDto.additionalActData?.parkedSpot ?: "",
        assignedTo = activityDto.assignedTo?.firstInitialDotLastName() ?: "",
        confirmOrderText = activityDto.customerOrderNumber?.takeLast(4).toString(),
        isDugOrder = activityDto.fulfillment?.type == FulfillmentType.DUG,
        isRxDug = activityDto.isRxDug(),
        isPharmacyServicingOrders = activityDto.rxDetails?.pharmacyServicingOrders.orFalse(),
        isAuthDugEnabled = activityDto.handShakeRequired == true,
        toteCount = activityDto.toteCount ?: 0,
        bagCount = activityDto.bagCount ?: 0,
        looseItemCount = activityDto.looseItemCount ?: 0,
        isDeliveryOrder = activityDto.fulfillment?.type == FulfillmentType.DELIVERY,
        provider = activityDto.partnerName ?: "N/A",
        driverName = activityDto.fullDriverName(),
        startTime = if (updatedVehicleInfo?.nextActExpStartTime != null) {
            updatedVehicleInfo.nextActExpStartTime
        } else {
            activityDto.nextActExpStartTime
        },
        isHandOffPreComplete = activityDto.status == ActivityStatus.PRE_COMPLETED,
        confirmOrderTime = confirmOrderTime ?: ZonedDateTime.now(),
        customerArrivalStatusUI = if (updatedVehicleInfo?.subStatus != null) {
            convertArrivalStatus(updatedVehicleInfo.subStatus)
        } else {
            convertArrivalStatus(activityDto.subStatus)
        },
        issueScanningBags = issueScanningBags,
        authenticatedPin = activityDto.authCode,
        isRegulated = activityDto.containerActivities?.any { it.regulated == true },
        fulfillmentType = activityDto.fulfillment,
        handshakeRequired = activityDto.handShakeRequired.orFalse(),
        minimumAgeRequired = activityDto.minimumAgeRequired,
        entityReference = activityDto.entityReference,
        rejectedItems = rejectedItems,
        rxDeliveryFailedReason = rxDeliveryFailedReason,
        customerInfoData = customerInfoData,
        bagsPerTempZoneParams = bagsPerTempZoneParams,
        createdOrders = activityDto.orderCount ?: "0",
        status = activityDto.status,
        cartType = activityDto.cartType,
        groceryDestageStartTimestamp = groceryDestageStartTimestamp,
        groceryDestageCompleteTimestamp = groceryDestageCompleteTimestamp,
        scheduledPickupTimestamp = activityDto.scheduledPickupTimestamp,
        rxOrderIds = activityDto.rxDetails?.rxOrderId,
        rxOrderStatus = activityDto.rxDetails?.orderStatus,
        orderSummary = activityDto.orderSummary?.mapNotNull { it?.toOrderSummary() },
        isCas = activityDto.isCas,
        is3p = activityDto.is3p,
        source = activityDto.source ?: "",
        isPartnerPickDug = activityDto.is3p == true && activityDto.fulfillment?.type == FulfillmentType.DUG,
        driverInfoHeader = if (activityDto.fulfillment?.type == FulfillmentType.DELIVERY) R.string.delivery_driver else R.string.pick_up_person,
        phoneNumber = activityDto.getPhoneNumber().orEmpty(),

        spotNumber = if (updatedVehicleInfo?.vehicleInfo?.parkedSpot.isNotNullOrEmpty()) {
            updatedVehicleInfo?.vehicleInfo?.parkedSpot
        } else {
            activityDto.vehicleInfo?.parkedSpot
        },
        vehicleInformation = if (updatedVehicleInfo?.vehicleInfo?.vehicleDetail.isNotNullOrEmpty()) {
            updatedVehicleInfo?.vehicleInfo?.vehicleDetail
        } else {
            activityDto.vehicleInfo?.vehicleDetail
        },
        vehicleLocation = if (updatedVehicleInfo?.vehicleInfo?.locationDetails.isNotNullOrEmpty()) {
            updatedVehicleInfo?.vehicleInfo?.locationDetails
        } else {
            activityDto.vehicleInfo?.locationDetails?.let { "\"$it\"" }
        },
        vehicleImageInfo = if (updatedVehicleInfo?.vehicleInfo?.color.isNotNullOrEmpty() && updatedVehicleInfo?.vehicleInfo?.type.isNotNullOrEmpty()) {
            updatedVehicleInfo?.vehicleInfo?.vehicleImageInfo()
        } else {
            activityDto.vehicleInfo?.vehicleImageInfo()
        },

        deliveryInstruction = activityDto.deliveryInstruction,
        feScreenStatus = if (updatedVehicleInfo?.feScreenStatus != null) {
            updatedVehicleInfo.feScreenStatus
        } else {
            activityDto.feScreenStatus
        },
        isCustomerBagPreference = activityDto.isCustomerBagPreference.orTrue(),
        isGiftLabelPrinted = isGiftLabelPrinted[activityDto.customerOrderNumber]
    )

    companion object {
        val context: Context by inject(Context::class.java)

        // Todo unit this code
        fun regulatedItemsList(activityDto: ActivityDto): List<HandOffRegulatedItem> =
            activityDto.containerItems?.flatMap { item ->
                item.pickedUpcCodes.orEmpty().filter {
                    it.regulated == true
                }.map { pickedItem ->
                    HandOffRegulatedItem(
                        description = (
                            if (pickedItem.isSubstitution == true) {
                                pickedItem.substituteItemDesc
                            } else {
                                item.itemDesc
                            }
                            ).orEmpty(),
                        totalQty = pickedItem.qty.orZero(),
                        itemId = if (pickedItem.isSubstitutionOrIssueScanning()) pickedItem.substituteItemId else item.itemId,
                        originalItemId = item.itemId,
                        upc = pickedItem.upc,
                        imageUrl = item.imageUrl
                    )
                }
            }.let { regulated ->
                // Groups list by description and then sum
                regulated?.distinctBy {
                    it.description
                }?.map { distinctItem ->
                    HandOffRegulatedItem(
                        description = distinctItem.description,
                        itemId = distinctItem.itemId,
                        originalItemId = distinctItem.originalItemId,
                        upc = distinctItem.upc,
                        totalQty = regulated.filter {
                            it.description == distinctItem.description
                        }.sumOf {
                            it.totalQty.orZero()
                        },
                        imageUrl = distinctItem.imageUrl
                    )
                }
            }.orEmpty()
    }
}

fun ActivityDto.getPhoneNumber(): String? {
    val number = if (fulfillment?.type == FulfillmentType.DELIVERY) driver?.phoneNumber.orEmpty() else contactPhoneNumber.orEmpty()
    return PhoneNumberUtils.formatNumber(number, Locale.getDefault().country) ?: null
}

fun VehicleInfoDto.vehicleImageInfo(): Pair<Int, Int>? {
    val style = when (color?.lowercase()) {
        VehicleColour.RED.color -> R.style.CarColorRed
        VehicleColour.BLUE.color -> R.style.CarColorBlue
        VehicleColour.BROWN.color -> R.style.CarColorBrown
        VehicleColour.BEIGE.color -> R.style.CarColorBeige
        VehicleColour.GREEN.color -> R.style.CarColorGreen
        VehicleColour.GRAY.color -> R.style.CarColorGray
        VehicleColour.SILVER.color -> R.style.CarColorSilver
        VehicleColour.BLACK.color -> R.style.CarColorBlack
        VehicleColour.WHITE.color -> R.style.CarColorWhite
        else -> 0
    }
    val car = when (type?.lowercase()) {
        VehicleType.SUV.type -> R.drawable.ic_suv
        VehicleType.CAR.type, VehicleType.SEDAN.type -> R.drawable.ic_sedan
        VehicleType.VAN.type -> R.drawable.ic_van
        VehicleType.TRUCK.type -> R.drawable.ic_pickup_truck
        else -> 0
    }
    return if (style.notZeroOrNull() && car.notZeroOrNull()) style to car else null
}
