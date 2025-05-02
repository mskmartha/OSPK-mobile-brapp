package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.network.NetworkAvailabilityManager
import com.albertsons.acupick.infrastructure.utils.stateFlowOf
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testNetworkAvailabilityManager: NetworkAvailabilityManager = mock {
    onBlocking { isConnected } doReturn stateFlowOf(true)
}

val testOfflineNetworkAvailabilityManager: NetworkAvailabilityManager = mock {
    onBlocking { isConnected } doReturn stateFlowOf(false)
}
