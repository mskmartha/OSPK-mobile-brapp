package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.CategoryStatus
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.alsoOnSuccess
import com.albertsons.acupick.data.model.asAppropriateFailure
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.Cancel1PLHandoffRequestDto
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.CompleteDropOffRequestDto
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.CreateUpdateDeviceInfoRequestDto
import com.albertsons.acupick.data.model.request.ErrorMessage
import com.albertsons.acupick.data.model.request.FetchOrderStatusRequestDto
import com.albertsons.acupick.data.model.request.NotificationRequestDto
import com.albertsons.acupick.data.model.request.PickupCompleteRequestDto
import com.albertsons.acupick.data.model.request.PreCompleteActivityRequestDto
import com.albertsons.acupick.data.model.request.PrintBagLabelRequestDto
import com.albertsons.acupick.data.model.request.RecordNotificationRequestDto
import com.albertsons.acupick.data.model.request.Get1PLTruckRemovalItemListRequestDto
import com.albertsons.acupick.data.model.request.RemoveItems1PLRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.RxRemoveRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.request.ScanRequestDto
import com.albertsons.acupick.data.model.request.UpdateDugArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.UpdateErBagRequestDto
import com.albertsons.acupick.data.model.request.UpdateErContBagRequestDto
import com.albertsons.acupick.data.model.request.UpdateOnePlArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.ValidatePalletRequestDto
import com.albertsons.acupick.data.model.response.AcknowledgedPickerDetailsDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ActivityDtoByCategoryDto
import com.albertsons.acupick.data.model.response.AppSummaryResponseDto
import com.albertsons.acupick.data.model.response.ArrivedInterjectionNotificationDto
import com.albertsons.acupick.data.model.response.BagAndLooseItemActivityDto
import com.albertsons.acupick.data.model.response.CustomerOrderStagingLocationDto
import com.albertsons.acupick.data.model.response.ErDto
import com.albertsons.acupick.data.model.response.FetchOrderStatusResponseDto
import com.albertsons.acupick.data.model.response.Remove1PLItemsResponseDto
import com.albertsons.acupick.data.model.response.ScanContActDto
import com.albertsons.acupick.data.model.response.ArrivalsCountDetailsDto
import com.albertsons.acupick.data.model.response.OnePlDto
import com.albertsons.acupick.data.model.response.ScanContDto
import com.albertsons.acupick.data.model.response.isAssigned
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ApsService
import retrofit2.Response
import timber.log.Timber
import java.time.ZonedDateTime

interface ApsRepository : Repository {
    suspend fun acceptNotification(notificationRequestDto: NotificationRequestDto): ApiResult<ActivityDto>
    suspend fun recordNotificationReceived(notificationRequestDto: RecordNotificationRequestDto): ApiResult<ArrivedInterjectionNotificationDto>
    suspend fun preCompleteActivity(preCompleteActivityRequestDto: PreCompleteActivityRequestDto): ApiResult<ActivityDto>
    suspend fun recordRemoveItems(removeItemsRequestDto: RemoveItemsRequestDto): ApiResult<Unit>
    suspend fun recordRxRemoveItems(rxRemoveItemsRequestDto: RxRemoveRequestDto): ApiResult<Unit>
    suspend fun pickupComplete(pickupCompleteRequestDto: PickupCompleteRequestDto): ApiResult<ActivityDto>
    suspend fun printBagLabels(actId: String): ApiResult<Unit>
    suspend fun printBagLabelsForConts(printBagLabelRequestDto: PrintBagLabelRequestDto): ApiResult<Unit>
    suspend fun rePrintToteAndLooseLabels(printBagLabelRequestDto: PrintBagLabelRequestDto): ApiResult<Unit>
    suspend fun printToteLabel(actId: String): ApiResult<Unit>
    suspend fun recordBagCount(updateErBagRequestDto: UpdateErBagRequestDto): ApiResult<BagAndLooseItemActivityDto>
    suspend fun completeDropOffActivity(completeDropOffRequestDto: CompleteDropOffRequestDto): ApiResult<ActivityDto>
    suspend fun scanContainer(scanRequestDto: ScanRequestDto): ApiResult<ScanContActDto>
    suspend fun scanContainers(scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto): ApiResult<ScanContDto>
    suspend fun complete1PLPickup(removeItems1PLRequestDto: RemoveItems1PLRequestDto): ApiResult<Unit>
    suspend fun cancel1PLHandoff(cancel1PLHandoffRequestDto: Cancel1PLHandoffRequestDto): ApiResult<Unit>
    suspend fun addBagCount(updateErContBagRequestDto: UpdateErContBagRequestDto): ApiResult<*>
    suspend fun searchActivities(
        siteId: String,
        userId: String,
        assigned: Boolean? = null,
        assignedToMe: Boolean? = null,
        open: Boolean? = null,
        pickUpReady: Boolean? = null,
        stageByTime: ZonedDateTime? = null,
        hideFresh: Boolean? = true,
    ): ApiResult<List<ActivityDtoByCategoryDto>>

    suspend fun searchCustomerPickupOrders(
        firstName: String? = null,
        lastName: String? = null,
        orderNumber: String? = null,
        siteId: String?,
        onlyPickupReady: Boolean?,
    ): ApiResult<List<ErDto>>

    suspend fun search1PlArrivalsOrders(siteId: String?): ApiResult<List<OnePlDto>>

    suspend fun pickUpActivityDetails(id: Long, loadCI: Boolean = false): ApiResult<ActivityDto>
    fun cachedPickUpActivityDetails(id: Long): ActivityDto?

    suspend fun getAppSummary(siteId: String, userId: String, counter: Int?): ApiResult<AppSummaryResponseDto>

    suspend fun confirmRxPickup(confirmRxPickupRequestDto: ConfirmRxPickupRequestDto): ApiResult<Unit>
    suspend fun cancelHandoff(cancelHandoffRequestDto: CancelHandoffRequestDto): ApiResult<Unit>
    suspend fun cancelHandoffs(cancelHandOffRequestDtoList: List<CancelHandoffRequestDto>): ApiResult<Unit>
    suspend fun updateDugArrivalStatus(request: UpdateDugArrivalStatusRequestDto): ApiResult<*>
    suspend fun updateOnePlArrivalStatus(request: UpdateOnePlArrivalStatusRequestDto): ApiResult<*>
    suspend fun assignUserToHandoffs(request: AssignUserWrapperRequestDto): ApiResult<List<ActivityDto>>
    suspend fun createUpdateDeviceInfo(request: CreateUpdateDeviceInfoRequestDto): ApiResult<Unit>
    suspend fun fetchOrderStatus(request: FetchOrderStatusRequestDto): ApiResult<List<FetchOrderStatusResponseDto>>
    suspend fun getCustomerOrderStagingLocation(erId: List<Long>): ApiResult<List<CustomerOrderStagingLocationDto>>
    suspend fun getAcknowledgedPickerDetails(userId: String): ApiResult<AcknowledgedPickerDetailsDto>
    suspend fun getCustomerArrivalsCount(siteId: String): ApiResult<ArrivalsCountDetailsDto>
    suspend fun setCustomerOrderStagingLocation(customerOrderStagingLocationDto: CustomerOrderStagingLocationDto): ApiResult<CustomerOrderStagingLocationDto>
    suspend fun validatePallet(vaLidatePalletRequestDto: ValidatePalletRequestDto, isDarkStore: Boolean?, isWineOrder: Boolean?): ApiResult<Unit>
    suspend fun get1PLTruckRemovalList(remove1PLItemsRequestDto: Get1PLTruckRemovalItemListRequestDto): ApiResult<Remove1PLItemsResponseDto>
    suspend fun logError(errorMessage: ErrorMessage): ApiResult<Unit>
    suspend fun printGiftLabel(erIds: List<Long>): ApiResult<Unit>
}

internal class ApsRepositoryImplementation(
    private val apsService: ApsService,
    private val pickRepository: PickRepository,
    private val itemProcessorRepository: ItemProcessorRepository,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
) : ApsRepository {

    private var activityDetailsMap = mutableMapOf<Long, ActivityDto>()

    override suspend fun acceptNotification(notificationRequestDto: NotificationRequestDto): ApiResult<ActivityDto> {
        return wrapExceptions("acceptNotification") {
            apsService.acceptNotification(notificationRequestDto).toResult()
        }
    }

    override suspend fun recordNotificationReceived(notificationRequestDto: RecordNotificationRequestDto): ApiResult<ArrivedInterjectionNotificationDto> {
        return wrapExceptions("recordNotificationReceived") {
            apsService.recordNotificationReceived(notificationRequestDto).toResult()
        }
    }

    override suspend fun completeDropOffActivity(completeDropOffRequestDto: CompleteDropOffRequestDto): ApiResult<ActivityDto> {
        return wrapExceptions("scanLocAndCompleteDropOffAct") {
            apsService.completeDropOffActivity(completeDropOffRequestDto).toResult()
        }
    }

    override suspend fun preCompleteActivity(preCompleteActivityRequestDto: PreCompleteActivityRequestDto): ApiResult<ActivityDto> {
        return wrapExceptions("preCompleteActivity") {
            apsService.preCompleteActivity(preCompleteActivityRequestDto).toResult()
        }
    }

    override suspend fun get1PLTruckRemovalList(remove1PLItemsRequestDto: Get1PLTruckRemovalItemListRequestDto): ApiResult<Remove1PLItemsResponseDto> {
        return wrapExceptions("get1PLtruckRemovalItemList") {
            apsService.get1PLTruckRemovalItemList(remove1PLItemsRequestDto).toResult()
        }
    }

    override suspend fun logError(errorMessage: ErrorMessage): ApiResult<Unit> {
        return wrapExceptions("logError") {
            apsService.logError(errorMessage).toEmptyResult()
        }
    }

    override suspend fun printGiftLabel(erIds: List<Long>): ApiResult<Unit> {
        return wrapExceptions("printGiftLabel") {
            apsService.printGiftLabel(erIds).toResult()
        }
    }

    override suspend fun recordRemoveItems(removeItemsRequestDto: RemoveItemsRequestDto): ApiResult<Unit> {
        return wrapExceptions("removeItems") {
            apsService.recordRemoveItems(removeItemsRequestDto).toEmptyResult()
        }
    }

    override suspend fun recordRxRemoveItems(rxRemoveItemsRequestDto: RxRemoveRequestDto): ApiResult<Unit> {
        return wrapExceptions("removeItems") {
            apsService.recordRxRemoveItems(rxRemoveItemsRequestDto).toEmptyResult()
        }
    }

    override suspend fun pickupComplete(pickupCompleteRequestDto: PickupCompleteRequestDto): ApiResult<ActivityDto> {
        return wrapExceptions("pickupComplete") {
            apsService.pickupComplete(pickupCompleteRequestDto).toResult()
        }
    }

    override suspend fun printBagLabels(actId: String): ApiResult<Unit> {
        return wrapExceptions("printBagLabels") {
            apsService.printBagLabels(actId).toEmptyResult()
        }
    }

    override suspend fun printBagLabelsForConts(printBagLabelRequestDto: PrintBagLabelRequestDto): ApiResult<Unit> {
        return wrapExceptions("printBagLabelForConts") {
            apsService.printBagLabelsForConts(printBagLabelRequestDto).toEmptyResult()
        }
    }

    override suspend fun rePrintToteAndLooseLabels(printBagLabelRequestDto: PrintBagLabelRequestDto): ApiResult<Unit> {
        return wrapExceptions("printBagLabelForConts") {
            apsService.rePrintToteAndLooseLabels(printBagLabelRequestDto).toEmptyResult()
        }
    }

    override suspend fun printToteLabel(actId: String): ApiResult<Unit> {
        return wrapExceptions("printToteLabel") {
            apsService.printToteLabel(actId).toEmptyResult()
        }
    }

    override suspend fun recordBagCount(updateErBagRequestDto: UpdateErBagRequestDto): ApiResult<BagAndLooseItemActivityDto> {
        return wrapExceptions("recordBagCount") {
            apsService.recordBagCount(updateErBagRequestDto).toResult()
        }
    }

    override suspend fun scanContainer(scanRequestDto: ScanRequestDto): ApiResult<ScanContActDto> {
        return wrapExceptions("recordBagCount") {
            apsService.scanContainer(scanRequestDto).toResult()
        }
    }

    override suspend fun scanContainers(scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto): ApiResult<ScanContDto> {
        return wrapExceptions("recordBagCount") {
            apsService.scanContainers(scanContainerWrapperRequestDto).toResult()
        }
    }

    override suspend fun complete1PLPickup(removeItems1PLRequestDto: RemoveItems1PLRequestDto): ApiResult<Unit> {
        return wrapExceptions("complete1PLPickup") {
            apsService.complete1PLPickup(removeItems1PLRequestDto).toResult()
        }
    }

    override suspend fun cancel1PLHandoff(cancel1PLHandoffRequestDto: Cancel1PLHandoffRequestDto): ApiResult<Unit> {
        return wrapExceptions("cancel1PLHandoff") {
            apsService.cancel1PLHandoff(cancel1PLHandoffRequestDto).toResult()
        }
    }

    override suspend fun searchActivities(
        siteId: String,
        userId: String,
        assigned: Boolean?,
        assignedToMe: Boolean?,
        open: Boolean?,
        pickUpReady: Boolean?,
        stageByTime: ZonedDateTime?,
        hideFresh: Boolean?,
    ): ApiResult<List<ActivityDtoByCategoryDto>> {
        return wrapExceptions("searchActivities") {
            apsService.searchActivities(
                siteId = siteId, assigned = assigned, assignedToMe = assignedToMe, open = open, userId = userId, pickUpReady = pickUpReady,
                stageByDate = stageByTime, hideFresh = hideFresh
            )
                .toResult().alsoOnSuccess {
                    // For requests that contain assignedToMe:
                    // 1. If the response returns an empty list, then clear out the pick list offline data to remove any stale data.
                    // 2. If the response returns a non-empty list, store knowledge of a known active pick list for the picker.
                    if (assignedToMe == true) {
                        val assignedToMePickLists = it.find { pickListOverview -> pickListOverview.category == CategoryStatus.ASSIGNED_TO_ME }?.data.orEmpty()
                        if (assignedToMePickLists.isEmpty()) {
                            Timber.d("[searchActivities] removing any stale, cached pick list data as assignedToMe has is empty")
                            pickRepository.clearAllData()
                            itemProcessorRepository.clearItemProcessorData()
                        } else {
                            pickRepository.setActivePickListActivityId(assignedToMePickLists.first().actId?.toString() ?: "")
                        }
                    }
                }
        }
    }

    override suspend fun searchCustomerPickupOrders(
        firstName: String?,
        lastName: String?,
        orderNumber: String?,
        siteId: String?,
        onlyPickupReady: Boolean?,
    ): ApiResult<List<ErDto>> {
        return wrapExceptions("searchCustomerPickupOrders") {
            apsService.searchCustomerPickupOrders(firstName = firstName, lastName = lastName, orderNo = orderNumber, siteId = siteId, onlyPickupReady = onlyPickupReady).toResult()
        }
    }

    override suspend fun search1PlArrivalsOrders(siteId: String?): ApiResult<List<OnePlDto>> {
        return wrapExceptions("search1PlArrivalsOrders") {
            apsService.search1PlArrivalsOrders(siteId = siteId).toResult()
        }
    }

    override suspend fun pickUpActivityDetails(id: Long, loadCI: Boolean): ApiResult<ActivityDto> {
        return wrapExceptions("pickUpActivityDetails") {
            val activityDetails = apsService.pickUpActivityDetails(id, loadCI)
            activityDetailsMap[id] = activityDetails.body() as ActivityDto
            activityDetails.toResult()
        }
    }

    override fun cachedPickUpActivityDetails(id: Long): ActivityDto? {
        return activityDetailsMap[id]
    }

    override suspend fun getAppSummary(siteId: String, userId: String, counter: Int?): ApiResult<AppSummaryResponseDto> {
        return wrapExceptions("appSummary") {
            apsService.getAppSummary(siteId = siteId, userId = userId, counter = counter).toResult().alsoOnSuccess {
                // For summary results that contain and activity currently assigned to the User:
                // 1. If the response returns an null, then clear out the pick list offline data to remove any stale data.
                // 2. If the response returns data, store knowledge of a known active pick list for the picker.
                if (it.isAssigned(userId)) {
                    val assignedToMePickLists = it.activity
                    if (it.activity == null) {
                        Timber.d("[searchActivities] removing any stale, cached pick list data as assignedToMe has is empty")
                        pickRepository.clearAllData()
                        itemProcessorRepository.clearItemProcessorData()
                    } else {
                        pickRepository.setActivePickListActivityId(assignedToMePickLists?.actId?.toString() ?: "")
                    }
                }
            }
        }
    }

    override suspend fun confirmRxPickup(confirmRxPickupRequestDto: ConfirmRxPickupRequestDto): ApiResult<Unit> {
        return wrapExceptions("cancelHandoff") {
            apsService.confirmRxPickup(confirmRxPickupRequestDto).toEmptyResult()
        }
    }

    override suspend fun cancelHandoff(cancelHandoffRequestDto: CancelHandoffRequestDto): ApiResult<Unit> {
        return wrapExceptions("cancelHandoff") {
            apsService.cancelHandoff(cancelHandoffRequestDto).toEmptyResult()
        }
    }

    override suspend fun cancelHandoffs(cancelHandOffRequestDtoList: List<CancelHandoffRequestDto>): ApiResult<Unit> {
        return wrapExceptions("cancelHandoffs") {
            apsService.cancelHandoffs(cancelHandOffRequestDtoList).toEmptyResult()
        }
    }

    override suspend fun addBagCount(updateErContBagRequestDto: UpdateErContBagRequestDto): ApiResult<*> {
        return wrapExceptions("addBagCount") {
            apsService.addBagCount(updateErContBagRequestDto).toEmptyResult()
        }
    }

    override suspend fun updateDugArrivalStatus(request: UpdateDugArrivalStatusRequestDto): ApiResult<*> {
        return wrapExceptions("updateDugArrivalStatus") {
            apsService.updateDugArrivalStatus(request).toEmptyResult()
        }
    }

    override suspend fun updateOnePlArrivalStatus(request: UpdateOnePlArrivalStatusRequestDto): ApiResult<*> {
        return wrapExceptions("updateDugArrivalStatus") {
            apsService.markDeliveryVanUnarrived(request).toEmptyResult()
        }
    }

    override suspend fun assignUserToHandoffs(request: AssignUserWrapperRequestDto): ApiResult<List<ActivityDto>> {
        return wrapExceptions("assignUserToActivities") {
            apsService.assignUserToHandoffs(request).toResult()
        }
    }

    override suspend fun createUpdateDeviceInfo(request: CreateUpdateDeviceInfoRequestDto): ApiResult<Unit> {
        return try {
            Timber.e("--------- FCM Registering device with backend createUpdateDeviceInfo call ")
            val result = apsService.createUpdateDeviceInfo(request)
            if (result.isSuccessful) {
                Timber.e("FCM Registering device call returned a success code")
            } else {
                val errorMessage = result.errorBody()?.toString() ?: ""
                Timber.e("FCM Registering device call returned a failure code $errorMessage")
            }
            result.toEmptyResult()
        } catch (e: Exception) {
            e.asAppropriateFailure().also { Timber.tag("ApsRepository").e(e, "FCM createUpdateDeviceInfo exception caught and converted to failure: $it") }
        }
        // Not using wrap exceptions for loggin purposes to debug notification problem
        // return wrapExceptions("createUpdateDeviceInfo") {
        //    apsService.createUpdateDeviceInfo(request).toEmptyResult()
        // }
    }

    override suspend fun fetchOrderStatus(request: FetchOrderStatusRequestDto): ApiResult<List<FetchOrderStatusResponseDto>> {
        return wrapExceptions("fetchOrderStatus") {
            apsService.fetchorderStatus(request).toResult()
        }
    }

    override suspend fun getCustomerOrderStagingLocation(erId: List<Long>): ApiResult<List<CustomerOrderStagingLocationDto>> {
        return wrapExceptions("fetchCustomerOrderStagingLocation") {
            apsService.getCustomerOrderStagingLocation(erId).toResult()
        }
    }

    override suspend fun setCustomerOrderStagingLocation(customerOrderStagingLocationDto: CustomerOrderStagingLocationDto): ApiResult<CustomerOrderStagingLocationDto> {
        return wrapExceptions("setCustomerOrderStagingLocation") {
            apsService.setCustomerOrderStagingLocation(customerOrderStagingLocationDto).toResult()
        }
    }

    override suspend fun getAcknowledgedPickerDetails(userId: String): ApiResult<AcknowledgedPickerDetailsDto> {
        return wrapExceptions("getAcknowledgedPickerDetails") {
            apsService.getAcknowledgedPickerDetails(userId).toResult()
        }
    }

    override suspend fun getCustomerArrivalsCount(siteId: String): ApiResult<ArrivalsCountDetailsDto> {
        return wrapExceptions("getCustomerArrivalsCount") {
            apsService.getCustomerArrivalsCount(siteId).toResult()
        }
    }

    override suspend fun validatePallet(vaLidatePalletRequestDto: ValidatePalletRequestDto, isDarkStore: Boolean?, isWineOrder: Boolean?): ApiResult<Unit> {
        return wrapExceptions("getAcknowledgedPickerDetails") {
            apsService.validatePallet(vaLidatePalletRequestDto, isDarkStore, isWineOrder).toEmptyResult()
        }
    }

    /** Delegates to [wrapExceptions], passing in the class name here instead of requiring it of all callers */
    private suspend fun <T : Any> wrapExceptions(methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        return wrapExceptions("ApsRepository", methodName, block)
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    private fun <T : Any> Response<T>.toEmptyResult(): ApiResult<Unit> {
        return responseToApiResultMapper.toEmptyResult(this)
    }
}
