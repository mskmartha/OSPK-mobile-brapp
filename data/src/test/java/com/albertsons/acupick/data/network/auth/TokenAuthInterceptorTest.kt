package com.albertsons.acupick.data.network.auth

import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.model.request.BasicAuthRequestDto
import com.albertsons.acupick.data.model.response.AuthUserDto
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.network.HeaderInterceptorMock
import com.albertsons.acupick.data.network.auth.token.AccessTokenDto
import com.albertsons.acupick.data.network.auth.token.TokenAuthInterceptor
import com.albertsons.acupick.data.network.auth.token.TokenAuthService
import com.albertsons.acupick.data.repository.CredentialsRepository
import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertWithMessage
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.Response
import timber.log.Timber

class TokenAuthInterceptorTest : BaseTest() {

    @Test
    fun authInterceptor() {
        runBlocking {
            var accessTokenDto = accessToken
            val response: Response<AccessTokenDto> = mock() {
                on { body() }.then { accessTokenDto }
            }
            val basicAuthRequestDto: BasicAuthRequestDto = mock {
                on { userId }.then { "" }
                on { password }.then { "" }
                on { grantType }.then { "password" }
                on { appId }.then { "APS_APP" }
                on { appName }.then { "APS_APP" }
            }
            val service: TokenAuthService = mock() {
                onBlocking { getToken(basicAuthRequestDto) }.doAnswer { invocation ->
                    accessTokenDto = AccessTokenDto(accessToken = "${invocation.getArgument<String>(0)} + ${invocation.getArgument<String>(1)}")
                    Timber.v("[authInterceptor TokenAuthService.getToken] accessTokenDto=$accessTokenDto")
                    response
                }
            }
            val credentialsRepository = mock<CredentialsRepository> {
                on { loadCredentials() } doReturn ValidCredentialModel("patentlychris@gmail.com", "password1")
                on { loadToken() } doAnswer { accessTokenDto.toAccessToken() }
            }
            val interceptor = TokenAuthInterceptor(service, credentialsRepository)

            val headerInterceptorMock = HeaderInterceptorMock()
            interceptor.intercept(headerInterceptorMock.getMockedChain())
            // Need to capture two arguments, can't use mockito-kotlin dsl
            val nameCaptor = argumentCaptor<String>()
            val valueCaptor = argumentCaptor<String>()
            verify(headerInterceptorMock.requestBuilder, times(1)).header(nameCaptor.capture(), valueCaptor.capture())
            Timber.v("[authInterceptor] header key=${nameCaptor.lastValue}, value=${valueCaptor.lastValue}")
            assertWithMessage("Header should be added with key 'Authorization'")
                .that(nameCaptor.lastValue)
                .isEqualTo("Authorization")
            assertWithMessage("Header value should be created based on username and password")
                .that(valueCaptor.lastValue)
                .isEqualTo("Bearer ${accessTokenDto.accessToken}")
        }
    }

    private val accessToken = AccessTokenDto(
        accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJ2aW5heWFrIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIiwidHJ1c3QiXSwiZXhwIjoxNTk1MjcxODQ5LCJ1c2VyIjp7ImlkIjoiNWVmOWJj" +
            "MDNjNGRiZjEzOWFjODdhMjFkIiwidXNlcklkIjpudWxsLCJmaXJzdE5hbWUiOiJWaW5heWFrIiwibGFzdE5hbWUiOiJCaGFyZHdhaiIsImJhbm5lcnMiOlsic3RyaW5nIl0sInNpdGVzIjpbIjEyMzQiXSwicm9sZXMiOlsiUGlja" +
            "2VyIl0sInBlcm1pc3Npb25zIjpbXX0sImF1dGhvcml0aWVzIjpbIlVTRVIiXSwianRpIjoiNjcyOGU3OGEtZDg1My00NTRlLWFkZTgtYjlkMmQyZjVjNjg4IiwiY2xpZW50X2lkIjoic2FmZXdheS1jbGllbnQifQ.Z6kZLqrjZdWOb" +
            "1mAvwxbie7ha30s7NiKA3zz9GfhuXE",
        tokenType = "bearer",
        refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJ2aW5heWFrIiwic2NvcGUiOlsicmVhZCIsIndyaXRlIiwidHJ1c3QiXSwiYXRpIjoiNjcyOGU3OGEtZDg1My00NTRlLWFkZTgtYjlkMmQy" +
            "ZjVjNjg4IiwiZXhwIjoxNTk1MjcxODQ5LCJ1c2VyIjp7ImlkIjoiNWVmOWJjMDNjNGRiZjEzOWFjODdhMjFkIiwidXNlcklkIjpudWxsLCJmaXJzdE5hbWUiOiJWaW5heWFrIiwibGFzdE5hbWUiOiJCaGFyZHdhaiIsImJhbm5lcn" +
            "MiOlsic3RyaW5nIl0sInNpdGVzIjpbIjEyMzQiXSwicm9sZXMiOlsiUGlja2VyIl0sInBlcm1pc3Npb25zIjpbXX0sImF1dGhvcml0aWVzIjpbIlVTRVIiXSwianRpIjoiNDlkNzYzYjktYmM1Yi00OTQ5LTgxZWMtYzZiODFiZDdj" +
            "NWM3IiwiY2xpZW50X2lkIjoic2FmZXdheS1jbGllbnQifQ.E7v8YNyaD_1z0nhz6uYHe9ea1GezYEnzXQhSUUVSlIk",
        expiresInSeconds = 3599,
        scope = "read write trust",
        user = AuthUserDto(
            id = "5ef9bc03c4dbf139ac87a21d",
            userId = "skuma73@safeway.com",
            firstName = "Sathish",
            lastName = "Kumar",
            banners = listOf("string"),
            sites = listOf(
                SiteDto(
                    siteId = "1234",
                    isDefault = true
                ),
                SiteDto(
                    siteId = "4321",
                    isDefault = false
                ),
                SiteDto(
                    siteId = "2468",
                    isDefault = false
                )
            ),
            roles = listOf("Picker"),
            permissions = listOf()
        ),
        jti = "6728e78a-d853-454e-ade8-b9d2d2f5c688"
    )
}
