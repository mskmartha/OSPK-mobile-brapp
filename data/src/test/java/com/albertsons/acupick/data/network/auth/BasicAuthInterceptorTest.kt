package com.albertsons.acupick.data.network.auth

import com.albertsons.acupick.data.model.ValidCredentialModel
import com.albertsons.acupick.data.network.HeaderInterceptorMock
import com.albertsons.acupick.data.network.auth.basic.BasicAuthInterceptor
import com.albertsons.acupick.data.repository.CredentialsRepository
import com.albertsons.acupick.data.test.BaseTest
import com.google.common.truth.Truth.assertWithMessage
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BasicAuthInterceptorTest : BaseTest() {

    @Test
    fun authInterceptor() {
        runBlocking {
            val credentialsRepository = mock<CredentialsRepository> {
                on { loadCredentials() } doReturn ValidCredentialModel("patentlychris@gmail.com", "password1")
            }
            val interceptor = BasicAuthInterceptor(credentialsRepository)

            val headerInterceptorMock = HeaderInterceptorMock()
            interceptor.intercept(headerInterceptorMock.getMockedChain())
            // Need to capture two arguments, can't use mockito-kotlin dsl
            val nameCaptor = argumentCaptor<String>()
            val valueCaptor = argumentCaptor<String>()
            verify(headerInterceptorMock.requestBuilder, times(1)).header(nameCaptor.capture(), valueCaptor.capture())
            assertWithMessage("Header should be added with key 'Authorization'")
                .that(nameCaptor.lastValue)
                .isEqualTo("Authorization")
            assertWithMessage("Header value should be base64 encoding of username and password")
                .that(valueCaptor.lastValue)
                .isEqualTo("Basic cGF0ZW50bHljaHJpc0BnbWFpbC5jb206cGFzc3dvcmQx")
        }
    }
}
