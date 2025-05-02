package com.albertsons.acupick.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.albertsons.acupick.config.api.Version
import com.albertsons.acupick.config.api.appVersion
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.ConfigFlag
import com.albertsons.acupick.data.model.ResponseToApiResultMapper
import com.albertsons.acupick.data.model.request.FeatureFlagAttributeDto
import com.albertsons.acupick.data.model.request.ConfigFlagRequest
import com.albertsons.acupick.data.model.wrapExceptions
import com.albertsons.acupick.data.network.ConfigService
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.coroutine.AlbApplicationCoroutineScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

interface ConfigRepository : Repository {

    /**
     * Fetch config flags by site ID
     *
     * @param siteId - site ID
     */
    suspend fun fetchConfigFlagsBySiteId(siteId: String)

    /**
     * Read config value via flag key
     *
     * @param flagKey - flag key name
     * @param default - default value
     * @return State flow boolean
     */
    fun readConfigValue(flagKey: String, default: Boolean = false): StateFlow<Boolean>
}

internal class ConfigRepositoryImpl(
    private val coroutineScope: AlbApplicationCoroutineScope,
    private val configService: ConfigService,
    private val context: Context,
    private val responseToApiResultMapper: ResponseToApiResultMapper
) : ConfigRepository, KoinComponent {

    // DI
    private val acuPickLogger: AcuPickLoggerInterface by inject()
    private val environmentRepository: EnvironmentRepository by inject()

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = CONFIG_FLAGS_PREFERENCE)

    private val moshi = Moshi.Builder().build()
    private val configFlagsAdapter: JsonAdapter<List<ConfigFlag>?> by lazy {
        moshi.adapter(Types.newParameterizedType(List::class.java, ConfigFlag::class.java), emptySet())
    }

    init {
        coroutineScope.launch {
            val json = readJsonFromAssets(context, CONFIG_FILE)
            json?.let {
                runCatching {
                    val configFlags = configFlagsAdapter.fromJson(it) ?: emptyList()
                    saveConfigFlags(configFlags)
                }
            }
        }
    }

    override suspend fun fetchConfigFlagsBySiteId(siteId: String) {
        val isConfigProduction = environmentRepository.selectedConfig.configEnvironmentConfig.isProd
        val environment = if (isConfigProduction) ENVIRONMENT_PROD else ENVIRONMENT_QA1
        val result = fetchConfigFlags(
            ConfigFlagRequest(
                attributes = FeatureFlagAttributeDto(
                    siteId = siteId,
                    environment = if (isConfigProduction) ENVIRONMENT_PROD else ENVIRONMENT_QA1
                )
            )
        )
        when (result) {
            is ApiResult.Success -> {
                saveConfigFlags(result.data)
            }
            is ApiResult.Failure -> {
                if (result is ApiResult.Failure.Server) {
                    val errorCode = result.error?.errorCode?.resolvedType
                    acuPickLogger.reportMetric("Failed to load config flags for siteId=$siteId on $environment, error code $errorCode")
                } else {
                    acuPickLogger.reportMetric("Failed to load config flags for siteId=$siteId on $environment")
                }
            }
        }
    }

    override fun readConfigValue(flagKey: String, default: Boolean): StateFlow<Boolean> {
        val featureFlagPreferenceKey = booleanPreferencesKey(flagKey)
        return context.dataStore.data
            .mapNotNull { preferences ->
                Timber.d("preferences[$flagKey] = ${preferences[featureFlagPreferenceKey]}, default = $default")
                preferences[featureFlagPreferenceKey] ?: default
            }
            .stateIn(coroutineScope, SharingStarted.Lazily, default)
    }

    private suspend fun fetchConfigFlags(configFlagRequest: ConfigFlagRequest): ApiResult<List<ConfigFlag>> {
        return wrapExceptions("fetchConfigFlags") {
            val path = if (configFlagRequest.attributes?.environment == ENVIRONMENT_QA1) "qapub/ESFF/api/v1/evalattrs" else "pub/ESFF/api/v1/evalattrs"
            configService.fetchConfigFlags(path, configFlagRequest).toResult()
        }
    }

    private fun readJsonFromAssets(context: Context, filePath: String): String? {
        try {
            val source = context.assets.open(filePath).source().buffer()
            return source.readByteString().string(Charset.forName(StandardCharsets.UTF_8.name()))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun saveConfigFlags(configFlags: List<ConfigFlag>) {
        configFlags.forEach { configFlag ->
            if (configFlag.minVersion == null ||
                Version(appVersion(context)) >= Version(configFlag.minVersion)
            ) {
                configFlag.featureFlagName?.let { flagKey ->
                    context.dataStore.edit { settings ->
                        val featureFlagPreferenceKey = booleanPreferencesKey(flagKey)
                        configFlag.featureFlagValue?.let {
                            settings[featureFlagPreferenceKey] = configFlag.featureFlagValue
                        }
                        Timber.d("[saveConfigFlags] key: ${configFlag.featureFlagName} value: ${configFlag.featureFlagValue}")
                    }
                }
            }
        }
    }

    /** Delegates to [wrapExceptions], passing in the class name here instead of requiring it of all callers */
    private suspend fun <T : Any> wrapExceptions(methodName: String, block: suspend () -> ApiResult<T>): ApiResult<T> {
        return wrapExceptions("ConfigRepository", methodName, block)
    }

    private fun <T : Any> Response<T>.toResult(): ApiResult<T> {
        return responseToApiResultMapper.toResult(this)
    }

    companion object {
        const val ENVIRONMENT_PROD = "prod"
        const val ENVIRONMENT_QA1 = "qa1"
        const val CONFIG_FILE = "configFlags.json"
        const val CONFIG_FLAGS_PREFERENCE = "configFlagsPreference"
    }
}
