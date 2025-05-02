package com.albertsons.acupick.test.mocks

import com.albertsons.acupick.data.environment.ApsEnvironmentType
import com.albertsons.acupick.data.environment.AuthEnvironmentType
import com.albertsons.acupick.data.environment.ConfigEnvironmentType
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.environment.ItemProcessorEnvironmentType
import com.albertsons.acupick.data.environment.OsccEnvironmentType
import com.albertsons.acupick.data.model.ApsEnvironmentConfig
import com.albertsons.acupick.data.model.AuthEnvironmentConfig
import com.albertsons.acupick.data.model.ComprehensiveEnvironmentConfig
import com.albertsons.acupick.data.model.ConfigEnvironmentConfig
import com.albertsons.acupick.data.model.ItemProcessorEnvironmentConfig
import com.albertsons.acupick.data.model.OsccEnvironmentConfig
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val testEnvironmentRepository: EnvironmentRepository = mock {
    on { selectedConfig } doReturn ComprehensiveEnvironmentConfig(
        configEnvironmentConfig = ConfigEnvironmentConfig(ConfigEnvironmentType.QA1, "", false),
        apsEnvironmentConfig = ApsEnvironmentConfig(ApsEnvironmentType.DEV, "", false),
        osccEnvironmentConfig = OsccEnvironmentConfig(OsccEnvironmentType.QA, ", false"),
        itemProcessorEnvironmentConfig = ItemProcessorEnvironmentConfig(ItemProcessorEnvironmentType.QA, ""),
        authEnvironmentConfig = AuthEnvironmentConfig(AuthEnvironmentType.QA, "")
    )
}
