package com.albertsons.acupick.data.network

import com.albertsons.acupick.data.model.request.ConfigFlagRequest
import com.albertsons.acupick.data.model.ConfigFlag
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface ConfigService {

    @POST(value = "abs/{path}")
    suspend fun fetchConfigFlags(
        @Path(value = "path", encoded = true) path: String,
        @Body configFlagRequest: ConfigFlagRequest
    ): Response<List<ConfigFlag>>
}
