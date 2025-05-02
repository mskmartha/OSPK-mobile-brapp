package com.albertsons.acupick.data.network

import com.albertsons.acupick.data.model.request.AddParticipantDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OsccService {
    @GET(value = "v2/getConversationToken")
    suspend fun getTwilioToken(
        @Query(value = "source") source: String = "acupick",
        @Query(value = "id") id: String,
    ): Response<String>

    @POST(value = "v2/addParticipant")
    suspend fun addParticipant(
        @Body addParticipantDto: AddParticipantDto,
    ): Response<Unit>
}
