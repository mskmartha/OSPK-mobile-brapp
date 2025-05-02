package com.albertsons.acupick

import com.albertsons.acupick.config.api.ConfigApi
import com.albertsons.acupick.ui.util.orFalse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Config flags feature using Config API.
 *
 * Eventually we want to combine data streams with siteDetails
 * feature flags witto avoid confusion and duplicate sources of truth.
 */
object AcuPickConfig : KoinComponent {
    // DI
    private val configApi: ConfigApi by inject()

    /**
     * Boolean config flag to enable/disable code unavailable experience on handoff page.
     *
     * Default value is true.
     */
    private const val ENABLE_CODE_UNAVAILABLE = "authDug_codeUnavailableCTAEnabled"
    private const val ENABLE_THRESHOLD_PRICE = "pricing_displayThresholdEnabled"
    private const val ENABLE_RX_DUG = "feature.rxDug.enabled"
    private const val ENABLE_CUSTOMER_ATTRIBUTES = "feature.catt.enabled"
    private const val ENABLE_BAG_BYPASS = "feature.bagBypass.enabled"

    val rxEnabled by lazy {
        MutableStateFlow(configApi.getBoolean(ENABLE_RX_DUG, true).orFalse())
    }
    val cattEnabled by lazy {
        MutableStateFlow(configApi.getBoolean(ENABLE_CUSTOMER_ATTRIBUTES, true).orFalse())
    }
    val bagBypassEnabled by lazy {
        MutableStateFlow(configApi.getBoolean(ENABLE_BAG_BYPASS, true).orFalse())
    }

    fun isCodeUnavailableEnabled(): Boolean = configApi.getBoolean(ENABLE_CODE_UNAVAILABLE, true)
    fun isCodeUnavailableEnabledAsFlow(): StateFlow<Boolean> = configApi.getBooleanAsFlow(ENABLE_CODE_UNAVAILABLE, true)

    fun isThresholdPriceEnabled(): Boolean = configApi.getBoolean(ENABLE_THRESHOLD_PRICE, true)
    fun isThresholdPriceEnabledAsFlow(): StateFlow<Boolean> = configApi.getBooleanAsFlow(ENABLE_THRESHOLD_PRICE, true)

    fun isRxDugEnabled(): Boolean = configApi.getBoolean(ENABLE_RX_DUG, true)
    fun isRxDugEnabledAsFlow(): StateFlow<Boolean> = configApi.getBooleanAsFlow(ENABLE_RX_DUG, true)
}
