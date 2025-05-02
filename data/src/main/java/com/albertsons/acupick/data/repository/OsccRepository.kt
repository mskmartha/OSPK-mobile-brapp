package com.albertsons.acupick.data.repository

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.request.AddParticipantDto
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.OsccService
import com.albertsons.acupick.infrastructure.utils.commonRetrying
import retrofit2.Response

interface OsccRepository : Repository {
    suspend fun getTwilioToken(
        userId: String,
    ): ApiResult<String>

    suspend fun addParticipant(
        conversationSid: String,
        userId: String,
    ): ApiResult<Unit>
}

class OsccRepositoryImpl(
    private val osccService: OsccService,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
) : OsccRepository {
    override suspend fun getTwilioToken(
        userId: String,
    ): ApiResult<String> {
        return commonRetrying {
            wrapExceptions("OsccRepositoryImpl", NetworkCalls.GET_TWILIO_TOKEN_FAILURE.value) {
                osccService.getTwilioToken(id = userId).toResult(NetworkCalls.GET_TWILIO_TOKEN_FAILURE.value)
            }
        }
    }

    override suspend fun addParticipant(
        conversationSid: String,
        userId: String,
    ): ApiResult<Unit> {
        return commonRetrying {
            wrapExceptions("OsccRepositoryImpl", NetworkCalls.ADD_PARTICIPANT_FAILURE.value) {
                osccService.addParticipant(AddParticipantDto(conversationSid, userId)).toEmptyResult(NetworkCalls.ADD_PARTICIPANT_FAILURE.value)
            }
        }
    }

    private fun <T : Any> Response<T>.toResult(networkallName: String): ApiResult<T> {
        return responseToApiResultMapper.toResult(this, networkallName)
    }

    private fun <T : Any> Response<T>.toEmptyResult(networkallName: String): ApiResult<Unit> {
        return responseToApiResultMapper.toEmptyResult(this, networkallName)
    }
}
