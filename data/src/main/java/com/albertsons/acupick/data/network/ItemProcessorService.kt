package com.albertsons.acupick.data.network

import com.albertsons.acupick.data.model.request.MissingItemLocationRequestDto
import com.albertsons.acupick.data.model.request.SyncOfflineMissingItemsLocationReqDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ItemProcessorService {
    @POST(value = "api/capturePrimaryLocation")
    suspend fun captureMissingItemLocation(
        @Body missingItemLocationRequestDto: MissingItemLocationRequestDto,
    ): Response<Unit>

    @POST(value = "api/syncOfflineItemProcessor")
    suspend fun syncOfflineMissingItemLocation(@Body syncOfflineMissingItemLocationReqDto: SyncOfflineMissingItemsLocationReqDto): Response<Unit>
}
