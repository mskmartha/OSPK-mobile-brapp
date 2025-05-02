package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.map
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.auth.token.TokenAuthService
import com.albertsons.acupick.infrastructure.utils.exhaustive
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response

interface TokenizedLdapRepository : Repository {
    val pickersLdapDetails: StateFlow<Map<String, String>>
    suspend fun getSitePickersLdapDetails(siteId: String): ApiResult<Unit>
}

internal class TokenizedLdapRepositoryImplementation(
    private val tokenAuthService: TokenAuthService,
    private val responseToApiResultMapper: ResponseToApiResultMapper,
    private val sharedPrefs: SharedPreferences
) : TokenizedLdapRepository {
    val moshi: Moshi = Moshi.Builder().build()
    private val mapAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))

    private val _pickersLdapDetails = MutableStateFlow(getPickersTokenizedLdapDetails())
    override val pickersLdapDetails: StateFlow<Map<String, String>>
        get() = _pickersLdapDetails

    override suspend fun getSitePickersLdapDetails(siteId: String): ApiResult<Unit> {
        val result = wrapExceptions("TokenizedLdapRepository", "getSitePickersLdapDetails") {
            tokenAuthService.getTokenizedLdapDetails(siteId).toResult()
        }
        when (result) {
            is ApiResult.Success -> {
                result.data.storeUserDetails.associateBy({ it.tokenizedLdapId }, { it.firstName ?: "" }).let { resultMap ->
                    storePickersTokenizedLdapDetails(resultMap)
                    _pickersLdapDetails.value = resultMap
                }
            }

            is ApiResult.Failure -> Unit // no-op
        }.exhaustive
        return result.map { ApiResult.Success(Unit) }
    }

    companion object {
        private const val SITE_PICKERS_TOKENIZED_LDP = "sitePickersTokenizedLdap"
    }

    private fun storePickersTokenizedLdapDetails(pickersinfo: Map<String, String>) {
        val json = mapAdapter.toJson(pickersinfo)
        sharedPrefs.edit().putString(SITE_PICKERS_TOKENIZED_LDP, json).apply()
    }
    private fun getPickersTokenizedLdapDetails(): Map<String, String> {
        val json = sharedPrefs.getString(SITE_PICKERS_TOKENIZED_LDP, null)
        return json?.let { mapAdapter.fromJson(it) } ?: emptyMap()
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }
}
