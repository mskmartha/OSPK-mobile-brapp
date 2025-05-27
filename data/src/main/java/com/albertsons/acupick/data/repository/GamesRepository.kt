package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.response.GameConfigDto
import com.albertsons.acupick.data.model.response.OnePlDto
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ApsService
import retrofit2.Response

interface GamesRepository : Repository {

    suspend fun getGameRewardsPoint(): ApiResult<GameConfigDto>
}

internal class GamesRepositoryImpl(
    private val apsService: ApsService,
    private val responseToApiResultMapper: ResponseToApiResultMapper
) : GamesRepository {

    override suspend fun getGameRewardsPoint(): ApiResult<GameConfigDto> {
        return wrapExceptions("getGameRewards") {
            apsService.getGameRewards().toResult()
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