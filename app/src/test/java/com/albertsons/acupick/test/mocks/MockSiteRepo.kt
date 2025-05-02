package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.response.SiteDetailsDto
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testSite: SiteDetailsDto = mock {
    on { siteId } doReturn "2941"
    on { isMultipleHandoffAllowed } doReturn false
}

val testMfcSite: SiteDetailsDto = mock {
    on { siteId } doReturn "2776"
    on { mfc } doReturn true
}

val testSiteRepo: SiteRepository = mock {
    on { siteDetails } doReturn stateFlowOf(testSite)
    on { concernTime } doReturn 60000
    on { warningTime } doReturn 720000
    onBlocking { getSiteDetails(any(), anyOrNull()) } doReturn ApiResult.Success(listOf(testSite))
}

val testMfcSiteRepo: SiteRepository = mock {
    on { siteDetails } doReturn stateFlowOf(testMfcSite)
    on { isMFCSite } doReturn true
    onBlocking { getSiteDetails(any(), anyOrNull()) } doReturn ApiResult.Success(listOf(testMfcSite))
}
