package com.albertsons.acupick.data.network

import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HeaderInterceptorMock {
    lateinit var requestBuilder: Request.Builder
    fun getMockedChain(): Interceptor.Chain {
        requestBuilder = mock { requestBuilder ->
            on { header(anyOrNull(), anyOrNull()) } doAnswer { invocation ->
                requestBuilder
            }
        }
        val request: Request = mock {
            on { newBuilder() } doReturn requestBuilder
        }
        val response: Response = mock {
            on { code } doReturn 200
        }
        return mock {
            on { request() } doReturn request
            on { proceed(anyOrNull()) } doReturn response
        }
    }
}
