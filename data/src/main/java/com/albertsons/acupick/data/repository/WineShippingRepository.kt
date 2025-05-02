package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.converters.toDtos
import com.albertsons.acupick.data.model.AddCountResponseDto
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.BoxCountPerOrder
import com.albertsons.acupick.data.model.BoxDetails
import com.albertsons.acupick.data.model.BoxInfoDto
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.request.AddBoxCountRequestDto
import com.albertsons.acupick.data.model.toDto
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ApsService
import retrofit2.Response

interface WineShippingRepository : Repository {
    suspend fun getBoxDetails(activityId: String): ApiResult<BoxInfoDto>
    suspend fun addBoxCount(activityId: String, boxTypeCountDtoList: List<BoxCountPerOrder>): ApiResult<AddCountResponseDto>
    suspend fun updateBoxWeight(activityId: String, boxWeightDtoList: List<BoxDetails>?): ApiResult<*>
    suspend fun printBoxShippingLabels(activityId: Int?, customerOrderNumber: Int?, referenceEntityId: Long?): ApiResult<Unit>
}

internal class WineShippingRepositoryImplementation(
    private val apsService: ApsService,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
) : WineShippingRepository {

    override suspend fun getBoxDetails(activityId: String): ApiResult<BoxInfoDto> {
        return wrapExceptions("acceptNotification") {
            apsService.getBoxDetails(activityId).toResult()
        }
    }

    override suspend fun addBoxCount(activityId: String, boxCountPerOrder: List<BoxCountPerOrder>): ApiResult<AddCountResponseDto> {
        return wrapExceptions("addBoxCount") {
            apsService.addBoxCount(AddBoxCountRequestDto(activityId = activityId.toInt(), boxCountPerOrder = boxCountPerOrder.toDtos())).toResult()
        }
    }

    override suspend fun updateBoxWeight(activityId: String, boxWeightDtoList: List<BoxDetails>?): ApiResult<*> {
        return wrapExceptions("updateBoxWeight") {
            apsService.updateBoxWeight(BoxInfoDto(activityId = activityId.toInt(), boxDetails = boxWeightDtoList?.map { it.toDto() })).toResult()
        }
    }

    override suspend fun printBoxShippingLabels(activityId: Int?, customerOrderNumber: Int?, referenceEntityId: Long?): ApiResult<Unit> {
        return wrapExceptions("printBagLabels") {
            apsService.printBoxShippingLabels(activityId, customerOrderNumber, referenceEntityId).toEmptyResult()
        }
    }

    /** Delegates to [wrapExceptions], passing in the class name here instead of requiring it of all callers */
    private suspend fun <T : Any> wrapExceptions(methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        return wrapExceptions("WineShippingRepository", methodName, block)
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    private fun <T : Any> Response<T>.toEmptyResult(): ApiResult<Unit> {
        return responseToApiResultMapper.toEmptyResult(this)
    }
}
