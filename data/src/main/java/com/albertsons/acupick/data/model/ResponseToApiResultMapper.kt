package com.albertsons.acupick.data.model

import com.albertsons.acupick.data.model.response.AuthApiLdapLoginError
import com.albertsons.acupick.data.model.response.FallbackApiError
import com.albertsons.acupick.data.model.response.ServerErrorDto
import com.albertsons.acupick.data.model.response.toServerErrorDto
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.json.JSONObject
import retrofit2.Response
import timber.log.Timber

/** Converts a retrofit response to a wrapped [ApiResult] response, handling parsing from the different flavors of error responses to the single [ServerErrorDto] */
interface ResponseToApiResultMapper {
    /** Convert from response to [ApiResult] of the response */
    fun <T : Any> toResult(response: Response<T>, networkCallName: String = ""): ApiResult<T>
    /** Convert from response to [ApiResult] of Unit of the response. Use when you don't care about the actual success type. */
    fun <T : Any> toEmptyResult(response: Response<T>, networkCallName: String = ""): ApiResult<Unit>
}

class ResponseToApiResultMapperImplementation(moshi: Moshi) : ResponseToApiResultMapper {

    private val serverErrorAdapter: JsonAdapter<ServerErrorDto> = moshi.adapter(ServerErrorDto::class.java)
    private val fallbackApiErrorAdapter: JsonAdapter<FallbackApiError> = moshi.adapter(FallbackApiError::class.java)
    private val authApiLdapLoginErrorAdapter: JsonAdapter<AuthApiLdapLoginError> = moshi.adapter(AuthApiLdapLoginError::class.java)

    override fun <T : Any> toResult(response: Response<T>, networkCallName: String): ApiResult<T> {
        return try {
            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) {
                        ApiResult.Success(body)
                    } else {
                        Timber.w("[toResult] Response body null")
                        ApiResult.Failure.GeneralFailure("null response body", networkCallName)
                    }
                }
                else -> {
                    Timber.w("[toResult] Api not successful: message ${response.message()} code: ${response.code()}")
                    ApiResult.Failure.Server(generateServerErrorDto(response), networkCallName)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "[toResult] Unknown api error {$e.localizedMessage} stackTrace: {$e.stackTrace}")
            ApiResult.Failure.GeneralFailure(e.localizedMessage ?: "", networkCallName)
        }
    }

    override fun <T : Any> toEmptyResult(response: Response<T>, networkCallName: String): ApiResult<Unit> {
        return try {
            when {
                response.isSuccessful -> ApiResult.Success(Unit)
                else -> {
                    Timber.w("[toEmptyResult] Api not successful: message ${response.message()} code: ${response.code()}")
                    ApiResult.Failure.Server(generateServerErrorDto(response), networkCallName)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "[toEmptyResult] Unknown api error {$e.localizedMessage} stackTrace: {$e.stackTrace}")
            ApiResult.Failure.GeneralFailure(e.localizedMessage ?: "", networkCallName)
        }
    }

    /** Determines the correct error response format, deserializes it, and converts it to a [ServerErrorDto] */
    private fun <T> generateServerErrorDto(response: Response<T>): ServerErrorDto {
        // Use the error body response unless it is null or blank (use empty json object then)
        val errorBody = response.errorBody()?.string().takeIf { it.isNotNullOrBlank() } ?: "{}"
        val errorBodyResponse = JSONObject(errorBody)
        val serverErrorDto = try {
            when {
                // Unique field for the ldap login error response
                errorBodyResponse.has("error_description") -> {
                    Timber.v("[generateServerErrorDto] parsing error response as AuthApiLdapLoginError")
                    authApiLdapLoginErrorAdapter.fromJson(errorBody)?.toServerErrorDto()
                }
                // Unique field for the fallback error response
                errorBodyResponse.has("timestamp") -> {
                    Timber.v("[generateServerErrorDto] parsing error response as FallbackApiError")
                    fallbackApiErrorAdapter.fromJson(errorBody)?.toServerErrorDto()
                }
                // Unique field for the server error dto response
                errorBodyResponse.has("errorCode") -> {
                    Timber.v("[generateServerErrorDto] parsing error response as ServerErrorDo")
                    serverErrorAdapter.fromJson(errorBody)
                }
                else -> {
                    Timber.w("[generateServerErrorDto] unknown error response")
                    null
                }
            } ?: ServerErrorDto()
        } catch (e: Exception) {
            Timber.w(e, "[generateServerErrorDto] problem parsing error response body")
            ServerErrorDto()
        }
        // Overwrite any present http error code and status with the retrofit response code and status
        return serverErrorDto.copy(httpErrorCode = response.code(), status = response.message())
    }
}
