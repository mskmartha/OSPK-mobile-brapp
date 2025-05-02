package com.albertsons.acupick.data.network.auth.token

import com.albertsons.acupick.data.model.request.BasicAuthRequestDto
import com.albertsons.acupick.data.model.request.LogoutRequestDto
import com.albertsons.acupick.data.model.response.StoreUsersLdapDTO
import com.albertsons.acupick.data.network.auth.const1
import com.albertsons.acupick.data.network.auth.const2
import com.albertsons.acupick.data.network.auth.getBasicAuthHeader
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/** Auth token related apis */
internal interface TokenAuthService {

    @POST(value = "ldapToken")
    suspend fun getToken(
        @Body basicAuthRequestDto: BasicAuthRequestDto,
        @Header("Authorization") header: String = getBasicAuthHeader(const1, const2),
    ): Response<AccessTokenDto>

    @POST(value = "invalidateToken")
    suspend fun invalidateToken(
        @Body logoutRequestDto: LogoutRequestDto,
        @Header("Authorization") header: String = getBasicAuthHeader(const1, const2),
    ): Response<Boolean>

    @POST(value = "refreshToken")
    suspend fun refreshToken(
        @Body ldapRefreshTokenRequestDto: LdapRefreshTokenRequestDto,
        @Header("Authorization") header: String = getBasicAuthHeader(const1, const2),
    ): Response<AccessTokenDto>

    @GET(value = "tokenizedLdapDetailsForSite")
    suspend fun getTokenizedLdapDetails(
        @Query(value = "siteId") siteId: String,
        @Header("Authorization") header: String = getBasicAuthHeader(const1, const2),
    ): Response<StoreUsersLdapDTO>
}
