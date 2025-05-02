package com.albertsons.acupick.config.api

import com.albertsons.acupick.data.repository.ConfigRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConfigApiImpl : ConfigApi, KoinComponent {
    private val configRepository: ConfigRepository by inject()

    override fun getBoolean(flagKey: String): Boolean = configRepository.readConfigValue(flagKey).value
    override fun getBoolean(flagKey: String, default: Boolean): Boolean = configRepository.readConfigValue(flagKey, default).value
    override fun getBooleanAsFlow(flagKey: String, default: Boolean): StateFlow<Boolean> = configRepository.readConfigValue(flagKey, default)
}
