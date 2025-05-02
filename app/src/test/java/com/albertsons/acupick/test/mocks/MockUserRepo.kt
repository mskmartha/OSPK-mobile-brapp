package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.model.User
import com.albertsons.acupick.data.model.response.SiteDto
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testUser: User = mock {
    on { selectedStoreId } doReturn "2941"
    on { sites } doReturn listOf(SiteDto("2941", true), SiteDto("1234", false), SiteDto("5678", false), SiteDto("0000", false))
    on { firstName } doReturn "First"
    on { lastName } doReturn "Last"
    on { userId } doReturn "8304636844"
    on { getStoreIds() } doReturn listOf("2941", "1234", "5678", "0000")
}

val testFailUser: User = mock {
    on { selectedStoreId } doReturn "glerp"
    on { sites } doReturn listOf(SiteDto("2941", true), SiteDto("1234", false), SiteDto("5678", false), SiteDto("0000", false))
    on { firstName } doReturn "First"
    on { lastName } doReturn "Last"
    on { userId } doReturn "8304636844"
    on { getStoreIds() } doReturn listOf("2941", "1234", "5678", "0000")
}

val testUserRepo: UserRepository = mock {
    on { user } doReturn stateFlowOf(testUser)
    on { isLoggedIn } doReturn stateFlowOf(true)
}
