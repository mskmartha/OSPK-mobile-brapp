package com.albertsons.acupick.data.network

import com.albertsons.acupick.data.model.AddCountResponseDto
import com.albertsons.acupick.data.model.BoxInfoDto
import com.albertsons.acupick.data.model.request.AddBoxCountRequestDto
import com.albertsons.acupick.data.model.request.AssignUserRequestDto
import com.albertsons.acupick.data.model.request.AssignUserWrapperRequestDto
import com.albertsons.acupick.data.model.request.Cancel1PLHandoffRequestDto
import com.albertsons.acupick.data.model.request.CancelHandoffRequestDto
import com.albertsons.acupick.data.model.request.CompleteDropOffRequestDto
import com.albertsons.acupick.data.model.request.ConfirmRxPickupRequestDto
import com.albertsons.acupick.data.model.request.CreateUpdateDeviceInfoRequestDto
import com.albertsons.acupick.data.model.request.ErrorMessage
import com.albertsons.acupick.data.model.request.FetchOrderStatusRequestDto
import com.albertsons.acupick.data.model.request.ItemPickRequestDto
import com.albertsons.acupick.data.model.request.ItemPickCompleteDto
import com.albertsons.acupick.data.model.request.NotificationRequestDto
import com.albertsons.acupick.data.model.request.PickCompleteRequestDto
import com.albertsons.acupick.data.model.request.PickupCompleteRequestDto
import com.albertsons.acupick.data.model.request.PreCompleteActivityRequestDto
import com.albertsons.acupick.data.model.request.PrintBagLabelRequestDto
import com.albertsons.acupick.data.model.request.ReassignDropOffRequestDto
import com.albertsons.acupick.data.model.request.RecordNotificationRequestDto
import com.albertsons.acupick.data.model.request.Get1PLTruckRemovalItemListRequestDto
import com.albertsons.acupick.data.model.request.RemoveItems1PLRequestDto
import com.albertsons.acupick.data.model.request.RemoveItemsRequestDto
import com.albertsons.acupick.data.model.request.RxRemoveRequestDto
import com.albertsons.acupick.data.model.request.ScanContainerWrapperRequestDto
import com.albertsons.acupick.data.model.request.ScanRequestDto
import com.albertsons.acupick.data.model.request.ShortPickRequestDto
import com.albertsons.acupick.data.model.request.SyncOfflinePickingRequestDto
import com.albertsons.acupick.data.model.request.UndoPickRequestDto
import com.albertsons.acupick.data.model.request.UndoShortRequestDto
import com.albertsons.acupick.data.model.request.UpdateDugArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.UpdateErBagRequestDto
import com.albertsons.acupick.data.model.request.UpdateErContBagRequestDto
import com.albertsons.acupick.data.model.request.UpdateOnePlArrivalStatusRequestDto
import com.albertsons.acupick.data.model.request.UserActivityLoginDto
import com.albertsons.acupick.data.model.request.UserActivityRequestDto
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
import com.albertsons.acupick.data.model.response.ItemDetailDto
import com.albertsons.acupick.data.model.response.ItemLocationDto
import com.albertsons.acupick.data.model.response.ItemUpcDto
import com.albertsons.acupick.data.model.response.PickItemDto
import com.albertsons.acupick.data.model.response.Remove1PLItemsResponseDto
import com.albertsons.acupick.data.model.response.ScanContActDto
import com.albertsons.acupick.data.model.response.ShortItemDto
import com.albertsons.acupick.data.model.response.SiteDetailsDto
import com.albertsons.acupick.data.model.response.SiteType
import com.albertsons.acupick.data.model.response.StagingSummaryDto
import com.albertsons.acupick.data.model.response.SubstitutionItemDetailsDto
import com.albertsons.acupick.data.model.response.ArrivalsCountDetailsDto
import com.albertsons.acupick.data.model.response.GameConfigDto
import com.albertsons.acupick.data.model.response.GamesPointsDto
import com.albertsons.acupick.data.model.response.OnePlDto
import com.albertsons.acupick.data.model.response.ScanContDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.ZonedDateTime

internal interface ApsService {

    @POST(value = "api/acceptNotification")
    suspend fun acceptNotification(@Body notificationRequestDto: NotificationRequestDto): Response<ActivityDto>

    @POST(value = "api/recordNotification")
    suspend fun recordNotificationReceived(@Body notificationRequestDto: RecordNotificationRequestDto): Response<ArrivedInterjectionNotificationDto>

    @POST(value = "api/assignUser")
    suspend fun assignUser(@Body assignUserRequestDto: AssignUserRequestDto): Response<ActivityDto>

    @POST(value = "api/preCompleteActivity")
    suspend fun preCompleteActivity(@Body preCompleteActivityRequestDto: PreCompleteActivityRequestDto): Response<ActivityDto>

    @POST(value = "api/pickupComplete")
    suspend fun pickupComplete(@Body pickupCompleteRequestDto: PickupCompleteRequestDto): Response<ActivityDto>

    @POST(value = "api/completeDropOffActivity")
    suspend fun completeDropOffActivity(@Body completeDropOffRequestDto: CompleteDropOffRequestDto): Response<ActivityDto>

    @POST(value = "api/unAssignPicker")
    suspend fun unAssignUser(@Query(value = "actId") actId: String): Response<Unit>

    @POST(value = "api/reassignDropOff")
    suspend fun reAssignUserStaging(@Body reAssign: ReassignDropOffRequestDto): Response<Unit>

    @POST(value = "api/completePickForStaging")
    suspend fun completePickForStaging(@Body pickCompleteRequestDto: PickCompleteRequestDto): Response<ActivityDto>

    @GET(value = "api/stagingSummary/{id}")
    suspend fun getStagingSummary(@Path(value = "id") id: String): Response<StagingSummaryDto>

    @GET(value = "api/activity/{id}")
    suspend fun getActivityDetails(
        @Path(value = "id") id: String,
        @Query(value = "loadCA") loadCa: Boolean,
        @Query(value = "loadIA") loadIa: Boolean,
        @Query(value = "loadMasterView") loadMasterView: Boolean? = null,
        @Query(value = "itemId") itemId: String? = null,
        @Query(value = "orderNumber") orderNumber: String? = null,
    ): Response<ActivityDto>

    @GET(value = "api/itemDetails")
    suspend fun getItemDetails(
        @Query(value = "siteId") siteId: String,
        @Query(value = "upcId") upcId: String,
        @Query(value = "pluCode") pluCode: String? = null,
        @Query(value = "actId") actId: Long? = null,
        @Query(value = "originalItemId") originalItemId: String? = null,
        @Query(value = "sellByWeightInd") sellByWeightInd: String? = null,
        @Query(value = "queryType") queryType: String? = null,
    ): Response<ItemDetailDto>

    @GET(value = "api/getAcknowledgedPickerDetails")
    suspend fun getAcknowledgedPickerDetails(
        @Query(value = "userId") userId: String,
    ): Response<AcknowledgedPickerDetailsDto>

    @GET(value = "api/getCustomerArrivalCount")
    suspend fun getCustomerArrivalsCount(
        @Query(value = "siteId") siteId: String,
    ): Response<ArrivalsCountDetailsDto>

    @GET(value = "api/substitutionItemDetails")
    suspend fun getSubstitutionItemDetailList(
        @Query(value = "actId") actId: String,
    ): Response<List<SubstitutionItemDetailsDto>>

    @GET(value = "api/getAllItemLoc")
    suspend fun getAllItemLocations(
        @Query(value = "siteId") siteId: String,
        @Query(value = "itemIds") itemIds: List<String>,
    ): Response<Map<String, List<ItemLocationDto>>>

    @GET(value = "api/getCustomerOrderStagingLocation")
    suspend fun getCustomerOrderStagingLocation(
        @Query(value = "erIds") erIds: List<Long>,
    ): Response<List<CustomerOrderStagingLocationDto>>

    @POST(value = "api/setCustomerOrderStagingLocation")
    suspend fun setCustomerOrderStagingLocation(
        @Body customerOrderStagingLocationDto: CustomerOrderStagingLocationDto,
    ): Response<CustomerOrderStagingLocationDto>

    @GET(value = "api/itemUpcList")
    suspend fun getItemUpcList(
        @Query(value = "siteId") siteId: String,
        @Query(value = "itemIds") itemIds: List<String>,
    ): Response<List<ItemUpcDto>>

    @POST(value = "api/printBagLabel")
    suspend fun printBagLabels(@Query(value = "actId") actId: String): Response<Unit>

    @POST(value = "api/printBagLabelForConts")
    suspend fun printBagLabelsForConts(@Body printBagLabelRequestDto: PrintBagLabelRequestDto): Response<Unit>

    @POST(value = "api/rePrintToteAndLooseLabels")
    suspend fun rePrintToteAndLooseLabels(@Body printBagLabelRequestDto: PrintBagLabelRequestDto): Response<Unit>

    @POST(value = "api/printToteLabel")
    suspend fun printToteLabel(@Query(value = "actId") actId: String): Response<Unit>

    @POST(value = "api/addBagCountContainer")
    suspend fun recordBagCount(@Body updateErBagRequestDto: UpdateErBagRequestDto): Response<BagAndLooseItemActivityDto>

    @POST(value = "api/recordPick")
    suspend fun recordPick(@Body itemPickRequestDto: ItemPickRequestDto): Response<List<PickItemDto>>

    @POST(value = "api/recordShortage")
    suspend fun recordShortage(@Body shortPickRequestDto: ShortPickRequestDto): Response<List<ShortItemDto>>

    @POST(value = "api/recordRemoveItems")
    suspend fun recordRemoveItems(@Body removeItemsRequestDto: RemoveItemsRequestDto): Response<List<ShortItemDto>>

    @POST(value = "api/recordRxRemoveItems")
    suspend fun recordRxRemoveItems(@Body rxRemoveRequestDto: RxRemoveRequestDto): Response<Unit>

    @POST(value = "api/scanContainer")
    suspend fun scanContainer(@Body scanRequestDto: ScanRequestDto): Response<ScanContActDto>

    @POST(value = "api/v2/scanContainers")
    suspend fun scanContainers(@Body scanContainerWrapperRequestDto: ScanContainerWrapperRequestDto): Response<ScanContDto>

    @POST(value = "api/completeOnePLPickup")
    suspend fun complete1PLPickup(@Body scanContainerWrapperRequestDto: RemoveItems1PLRequestDto): Response<Unit>

    @PUT(value = "api/unassignDeliveryPickup")
    suspend fun cancel1PLHandoff(@Body cancel1PLHandoffRequestDto: Cancel1PLHandoffRequestDto): Response<Unit>

    /** https://confluence.safeway.com/display/EOM/Activity+Search */
    @GET(value = "api/searchActivities")
    suspend fun searchActivities(
        /** When true, fetch pick lists assigned to given user id and others (to be returned in the response) */
        @Query(value = "assigned") assigned: Boolean?,
        /** When true, fetch pick lists assigned to given user id (to be returned in the response) */
        @Query(value = "assignedToMe") assignedToMe: Boolean?,
        /** When true, fetch open pick lists (to be returned in the response) */
        @Query(value = "open") open: Boolean?,
        @Query(value = "siteId") siteId: String,
        @Query(value = "userId") userId: String?,
        /** When true, fetch completed staging activities (to be returned in the response) */
        @Query(value = "pickUpReady") pickUpReady: Boolean?,
        /** When set, fetch pick lists on specified date. Backend default (when null) is current date */
        @Query(value = "stageByDate") stageByDate: ZonedDateTime?,
        @Query(value = "hideFresh") hideFresh: Boolean?,
    ): Response<List<ActivityDtoByCategoryDto>>

    @GET(value = "api/sites")
    suspend fun getSiteDetails(
        @Query(value = "siteId") siteId: String,
        @Query(value = "siteType") siteType: SiteType?,
    ): Response<List<SiteDetailsDto>>

    @POST(value = "api/syncOfflinePicking")
    suspend fun syncOfflineOperations(@Body syncOfflinePickingRequestDto: SyncOfflinePickingRequestDto): Response<ActivityDto>

    @POST(value = "api/undoPickList")
    suspend fun undoPicks(@Body undoPickRequestDto: List<UndoPickRequestDto>): Response<List<PickItemDto>>

    @POST(value = "api/undoShortage")
    suspend fun undoShortage(@Body undoShortRequestDto: UndoShortRequestDto): Response<List<ShortItemDto>>

    @GET(value = "api/searchCustomerPickupOrders")
    suspend fun searchCustomerPickupOrders(
        @Query(value = "firstName") firstName: String?,
        @Query(value = "lastName") lastName: String?,
        @Query(value = "orderNo") orderNo: String?,
        @Query(value = "siteId") siteId: String?,
        @Query(value = "onlyPickupReady") onlyPickupReady: Boolean?,
    ): Response<List<ErDto>>

    @GET(value = "api/search1PLArrivals")
    suspend fun search1PlArrivalsOrders(
        @Query(value = "siteId") siteId: String?
    ): Response<List<OnePlDto>>

    @GET(value = "api/pickUpActDetailsByErId/{id}")
    suspend fun pickUpActivityDetails(
        @Path(value = "id") id: Long?,
        @Query(value = "loadCI") loadCI: Boolean?,
    ): Response<ActivityDto>

    @POST(value = "api/onePlTruckRemovalItemList")
    suspend fun get1PLTruckRemovalItemList(@Body removalRemove1PLItemsRequestDto: Get1PLTruckRemovalItemListRequestDto): Response<Remove1PLItemsResponseDto>

    @POST(value = "api/confirmRxPickup")
    suspend fun confirmRxPickup(@Body confirmRxPickupRequestDto: ConfirmRxPickupRequestDto): Response<Unit>

    @GET(value = "api/appSummary")
    suspend fun getAppSummary(
        @Query(value = "siteId") siteId: String?,
        @Query(value = "userId") userId: String?,
        @Query(value = "counter") counter: Int?,
    ): Response<AppSummaryResponseDto>

    @POST(value = "api/cancelHandoff")
    suspend fun cancelHandoff(@Body cancelHandoffRequestDto: CancelHandoffRequestDto): Response<Unit>

    @POST(value = "api/cancelHandoffs")
    suspend fun cancelHandoffs(@Body cancelHandOffRequestDtoList: List<CancelHandoffRequestDto>): Response<Unit>

    @POST(value = "api/UpdateDUGArrivalStatus")
    suspend fun updateDugArrivalStatus(@Body updateDugArrivalStatusRequestDto: UpdateDugArrivalStatusRequestDto): Response<Unit>

    @PUT(value = "api/markDeliveryVanUnarrived")
    suspend fun markDeliveryVanUnarrived(@Body updateOnePlArrivalStatusRequestDto: UpdateOnePlArrivalStatusRequestDto): Response<Unit>

    @POST(value = "api/addExtraBagCountContainer")
    suspend fun addBagCount(@Body updateErContBagRequestDto: UpdateErContBagRequestDto): Response<Unit>

    @POST(value = "api/assignUserToHandoffs")
    suspend fun assignUserToHandoffs(@Body assignUserWrapperRequestDto: AssignUserWrapperRequestDto): Response<List<ActivityDto>>

    @POST(value = "api/createUpdateDeviceInfo")
    suspend fun createUpdateDeviceInfo(@Body createUpdateDeviceInfoRequestDto: CreateUpdateDeviceInfoRequestDto): Response<Unit>

    @POST(value = "api/userActivityLogin")
    suspend fun logUserActivityLogin(@Body userActivityRequestDto: UserActivityRequestDto): Response<UserActivityLoginDto>

    @POST(value = "api/userActivityLogout")
    suspend fun logUserActivityLogout(@Body userActivityRequestDto: UserActivityRequestDto): Response<Unit>

    @POST(value = "api/fetchOrderStatus")
    suspend fun fetchorderStatus(@Body fetchOrderStatusRequestDto: FetchOrderStatusRequestDto): Response<List<FetchOrderStatusResponseDto>>

    // WineShiping
    @GET(value = "api/getBoxDetails/{activityId}")
    suspend fun getBoxDetails(@Path(value = "activityId") activityId: String): Response<BoxInfoDto>

    @POST(value = "api/addBoxCount")
    suspend fun addBoxCount(@Body addBoxCountRequestDto: AddBoxCountRequestDto): Response<AddCountResponseDto>

    @PUT(value = "api/updateBoxWeight")
    suspend fun updateBoxWeight(@Body boxInfoDto: BoxInfoDto): Response<Unit>

    @POST(value = "api/printBoxShippingLabels")
    suspend fun printBoxShippingLabels(
        @Query(value = "activityId") activityId: Int?,
        @Query(value = "customerOrderNumber") customerOrderNumber: Int?,
        @Query(value = "referenceEntityId") referenceEntityId: Long?,
    ): Response<Unit>

    @POST(value = "api/validatePallet")
    suspend fun validatePallet(
        @Body validatePalletRequestDto: ValidatePalletRequestDto,
        @Query(value = "isDarkStore") isDarkStore: Boolean?,
        @Query(value = "isWineOrder") isWineOrder: Boolean?,
    ): Response<Unit>

    @PUT(value = "api/completePick/itemActivity/{itemactivityId}")
    suspend fun completePickClicked(
        @Path(value = "itemactivityId") itemactivityId: String,
    ): Response<Unit>

    @POST(value = "api/recordItemPickComplete")
    suspend fun recordItemPickComplete(
        @Body itemPickCompleteDto: ItemPickCompleteDto,
    ): Response<Unit>

    @POST(value = "api/printGiftLabel")
    suspend fun printGiftLabel(
        @Query(value = "erIds") erIds: List<Long>
    ): Response<Unit>

    @POST(value = "api/exceptionEvents")
    suspend fun logError(
        @Body errorMessage: ErrorMessage,
    ): Response<Unit>


    @GET("https://acupickgame.free.beeceptor.com/oceg-game-services/gamification/player/rules/smar602")
    suspend fun getGameRewards(): Response<GameConfigDto>

    @GET("https://acupickgame.free.beeceptor.com/oceg-game-services/getMyScoreInfo/v1?playerId=smar602")
    suspend fun getTotalPoints(): Response<GamesPointsDto>
}
