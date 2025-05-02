package com.albertsons.acupick.ui.fieldservices

import android.app.Application
import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.model.response.HealthCheckResponse
import com.albertsons.acupick.data.network.logging.LoggingDataProvider
import com.albertsons.acupick.data.network.logging.HeaderInterceptor
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.coroutine.DispatcherProvider
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.MainActivityViewModel
import com.jakewharton.processphoenix.ProcessPhoenix
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.picasso.Callback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException
import java.lang.Thread.sleep

class FieldServicesViewModel(
    private val app: Application,
    private val environmentRepository: EnvironmentRepository,
    private val activityViewModel: MainActivityViewModel,
    dispatcherProvider: DispatcherProvider
) : BaseViewModel(app), KoinComponent {
    // DI
    private val loggingDataProvider: LoggingDataProvider by inject()
    private val logger: AcuPickLoggerInterface by inject()
    private val context: Context by inject()

    // Interceptor
    private val interceptor = HeaderInterceptor(loggingDataProvider, logger, environmentRepository, context)

    // Client for OkHttp
    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    // Moshi for parsing
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter: JsonAdapter<HealthCheckResponse> = moshi.adapter(HealthCheckResponse::class.java)

    // Two way bindings
    val typedAuthUrl: MutableLiveData<String> = MutableLiveData(environmentRepository.selectedConfig.authEnvironmentConfig.baseAuthUrl)
    val typedAuthUrlEnabled = typedAuthUrl.map { it.isNotNullOrEmpty() }
    val typedApsUrl: MutableLiveData<String> = MutableLiveData(environmentRepository.selectedConfig.apsEnvironmentConfig.baseApsUrl)
    val typedApsUrlEnabled = typedAuthUrl.map { it.isNotNullOrEmpty() }
    val typedOsccUrl: MutableLiveData<String> = MutableLiveData(environmentRepository.selectedConfig.osccEnvironmentConfig.baseOsccUrl)
    val typedOsccUrlEnabled = typedOsccUrl.map { it.isNotNullOrEmpty() }
    val typedItemProcessorUrl: MutableLiveData<String> = MutableLiveData(environmentRepository.selectedConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl)
    val typedItemProcessorUrlEnabled = typedItemProcessorUrl.map { it.isNotNullOrEmpty() }
    val typedConfigUrl: MutableLiveData<String> = MutableLiveData(environmentRepository.selectedConfig.configEnvironmentConfig.baseConfigUrl)
    val typedConfigUrlEnabled = typedConfigUrl.map { it.isNotNullOrEmpty() }
    val typedImageUrl: MutableLiveData<String> = MutableLiveData(DEFAULT_IMAGE_URL)
    val typedImageUrlEnabled = typedAuthUrl.map { it.isNotNullOrEmpty() }

    /** Contains [typedImageUrl] value when [onStartTestCtaClicked] executes. */
    val displayedImageUrl: LiveData<String> = MutableLiveData()

    val authUrlOperationState: LiveData<FieldServiceOperationState> = MutableLiveData(FieldServiceOperationState.Unknown)
    val apsUrlOperationState: LiveData<FieldServiceOperationState> = MutableLiveData(FieldServiceOperationState.Unknown)
    val osccUrlOperationState: LiveData<FieldServiceOperationState> = MutableLiveData(FieldServiceOperationState.Unknown)
    val itemProcessorUrlOperationState: LiveData<FieldServiceOperationState> = MutableLiveData(FieldServiceOperationState.Unknown)
    val imageUrlOperationState: LiveData<FieldServiceOperationState> = MutableLiveData(FieldServiceOperationState.Unknown)
    val displayImageVisibility = imageUrlOperationState.map { if (it == FieldServiceOperationState.Success) View.VISIBLE else View.INVISIBLE }
    private val imageUrlLoadInProgress: LiveData<Boolean> = MutableLiveData(false)
    private val authEnvironmentLoadInProgress: LiveData<Boolean> = MutableLiveData(false)
    private val apsEnvironmentLoadInProgress: LiveData<Boolean> = MutableLiveData(false)
    private val osccEnvironmentLoadInProgress: LiveData<Boolean> = MutableLiveData(false)
    private val itemProcessorEnvironmentLoadInProgress: LiveData<Boolean> = MutableLiveData(false)

    /** True when there are no in progress tests (all have finished with a success or failure) */
    private val allTestsComplete: Flow<Boolean> = combine(
        imageUrlLoadInProgress.asFlow(),
        authEnvironmentLoadInProgress.asFlow(),
        apsEnvironmentLoadInProgress.asFlow(),
        osccEnvironmentLoadInProgress.asFlow(),
        itemProcessorEnvironmentLoadInProgress.asFlow()
    ) { imageUrlLoadInProgress, authEnvironmentLoadInProgress, apsEnvironmentLoadInProgress, osccEnvironmentLoadInProgress, itemProcessorEnvironmentLoadInProgress ->
        !imageUrlLoadInProgress && !authEnvironmentLoadInProgress && !apsEnvironmentLoadInProgress && !osccEnvironmentLoadInProgress && !itemProcessorEnvironmentLoadInProgress
    }

    // State Flag used to force restarts
    private var restart = false

    init {
        viewModelScope.launch(dispatcherProvider.IO) {
            // Wait for all tests to complete before removing the loading indicator
            allTestsComplete.filter { true }.collect {
                activityViewModel.setLoadingState(isLoading = false)
            }
        }
    }

    // Helpers
    private fun resetStatusViews() {
        authUrlOperationState.postValue(FieldServiceOperationState.Unknown)
        apsUrlOperationState.postValue(FieldServiceOperationState.Unknown)
        osccUrlOperationState.postValue(FieldServiceOperationState.Unknown)
        imageUrlOperationState.postValue(FieldServiceOperationState.Unknown)
        itemProcessorUrlOperationState.postValue(FieldServiceOperationState.Unknown)
    }

    private fun ensureTrailingSlash(path: String) = if (path.endsWith("/")) path else "$path/"

    private fun verifyResponse(response: Response, successStatus: String) = if (response.isSuccessful) {
        if (adapter.fromJson(response.body?.string() ?: "")?.status == successStatus) FieldServiceOperationState.Success else FieldServiceOperationState.Failure
    } else {
        FieldServiceOperationState.Failure
    }

    // /////////////////////////////////////////////////////////////////////////
    // Click actions
    // /////////////////////////////////////////////////////////////////////////
    fun onStartTestCtaClicked() {
        Timber.v("[onStartTestCtaClicked]")

        // Reset status for loading
        activityViewModel.setLoadingState(isLoading = true, blockUi = true)
        resetStatusViews()

        // Image loading test
        imageUrlLoadInProgress.postValue(true)
        displayedImageUrl.postValue(typedImageUrl.value.orEmpty())

        // Endpoint tests
        testAuthEndpoint()
        testApsEndpoint()
        testOsccEndpoint()
        testItemProcessorEndpoint()
    }

    fun onResetCtaClicked() {
        Timber.v("[onResetCtaClicked]")
        resetStatusViews()

        // If value needs reset, make changes and set flag for force restart
        if (ensureTrailingSlash(typedApsUrl.value ?: "") != environmentRepository.preOverrideConfig.apsEnvironmentConfig.baseApsUrl) {
            environmentRepository.overrideApsEnvironment("")
            typedApsUrl.postValue(environmentRepository.preOverrideConfig.apsEnvironmentConfig.baseApsUrl)
            restart = true
        }

        if (ensureTrailingSlash(typedOsccUrl.value ?: "") != environmentRepository.preOverrideConfig.osccEnvironmentConfig.baseOsccUrl) {
            environmentRepository.overrideApsEnvironment("")
            typedOsccUrl.postValue(environmentRepository.preOverrideConfig.osccEnvironmentConfig.baseOsccUrl)
            restart = true
        }

        // If value needs reset, make changes and set flag for force restart
        if (ensureTrailingSlash(typedAuthUrl.value ?: "") != environmentRepository.preOverrideConfig.authEnvironmentConfig.baseAuthUrl) {
            environmentRepository.overrideAuthEnvironment("")
            typedAuthUrl.postValue(environmentRepository.preOverrideConfig.authEnvironmentConfig.baseAuthUrl)
            restart = true
        }

        // If value needs reset, make changes and set flag for force restart
        if (ensureTrailingSlash(typedConfigUrl.value ?: "") != environmentRepository.preOverrideConfig.configEnvironmentConfig.baseConfigUrl) {
            environmentRepository.overrideConfigEnvironment("")
            typedConfigUrl.postValue(environmentRepository.preOverrideConfig.configEnvironmentConfig.baseConfigUrl)
            restart = true
        }

        // If value needs reset, make changes and set flag for force restart
        if (ensureTrailingSlash(typedItemProcessorUrl.value ?: "") != environmentRepository.preOverrideConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl) {
            environmentRepository.overrideItemProcessorEnvironment("")
            typedItemProcessorUrl.postValue(environmentRepository.preOverrideConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl)
            restart = true
        }

        typedImageUrl.postValue(DEFAULT_IMAGE_URL)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Health checks
    // /////////////////////////////////////////////////////////////////////////

    private fun testAuthEndpoint() {
        authEnvironmentLoadInProgress.postValue(true)
        try {
            client.newCall(
                Request.Builder().url(ensureTrailingSlash(typedAuthUrl.value ?: "") + healthEndpoint)
                    .build()
            ).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    authEnvironmentLoadInProgress.postValue(false)
                    authUrlOperationState.postValue(FieldServiceOperationState.Failure)
                }

                override fun onResponse(call: Call, response: Response) {
                    authEnvironmentLoadInProgress.postValue(false)
                    authUrlOperationState.postValue(verifyResponse(response, AUTH_SUCCESS_STATUS))
                }
            })
        } catch (e: Exception) {
            authEnvironmentLoadInProgress.postValue(false)
            authUrlOperationState.postValue(FieldServiceOperationState.Failure)
        }
    }

    private fun testApsEndpoint() {
        apsEnvironmentLoadInProgress.postValue(true)
        try {
            client.newCall(
                Request.Builder().url(ensureTrailingSlash(typedApsUrl.value ?: "") + healthEndpoint).build()
            ).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    apsEnvironmentLoadInProgress.postValue(false)
                    apsUrlOperationState.postValue(FieldServiceOperationState.Failure)
                }

                override fun onResponse(call: Call, response: Response) {
                    apsEnvironmentLoadInProgress.postValue(false)
                    apsUrlOperationState.postValue(verifyResponse(response, APS_SUCCESS_STATUS))
                }
            })
        } catch (e: Exception) {
            apsEnvironmentLoadInProgress.postValue(false)
            apsUrlOperationState.postValue(FieldServiceOperationState.Failure)
        }
    }

    private fun testOsccEndpoint() {
        osccEnvironmentLoadInProgress.postValue(true)
        try {
            client.newCall(
                Request.Builder().url(ensureTrailingSlash(typedOsccUrl.value ?: "") + healthCheckOsccEndpoint).build()
            ).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.d("testOsccEndpoint onFailure $e, ")
                    osccEnvironmentLoadInProgress.postValue(false)
                    osccUrlOperationState.postValue(FieldServiceOperationState.Failure)
                }

                override fun onResponse(call: Call, response: Response) {
                    osccEnvironmentLoadInProgress.postValue(false)
                    osccUrlOperationState.postValue(verifyResponse(response, OSCC_SUCCESS_STATUS))
                }
            })
        } catch (e: Exception) {
            Timber.d("testOsccEndpoint catch $e, ")
            osccEnvironmentLoadInProgress.postValue(false)
            osccUrlOperationState.postValue(FieldServiceOperationState.Failure)
        }
    }

    private fun testItemProcessorEndpoint() {
        itemProcessorEnvironmentLoadInProgress.postValue(true)
        try {
            client.newCall(
                Request.Builder().url(ensureTrailingSlash(typedItemProcessorUrl.value ?: "") + healthCheckItemProcessorEndpoint).build()
            ).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.d("testItemProcessorEndpoint onFailure $e, ")
                    itemProcessorEnvironmentLoadInProgress.postValue(false)
                    itemProcessorUrlOperationState.postValue(FieldServiceOperationState.Failure)
                }

                override fun onResponse(call: Call, response: Response) {
                    itemProcessorEnvironmentLoadInProgress.postValue(false)
                    itemProcessorUrlOperationState.postValue(verifyResponse(response, ITEM_PROCESSOR_SUCCESS_STATUS))
                }
            })
        } catch (e: Exception) {
            Timber.d("testItemProcessorEndpoint catch $e, ")
            itemProcessorEnvironmentLoadInProgress.postValue(false)
            itemProcessorUrlOperationState.postValue(FieldServiceOperationState.Failure)
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Callbacks
    // /////////////////////////////////////////////////////////////////////////
    fun saveBaseUrls() {
        if (apsUrlOperationState.value == FieldServiceOperationState.Success) {
            // Only save changes
            if (ensureTrailingSlash(typedApsUrl.value ?: "") != environmentRepository.selectedConfig.apsEnvironmentConfig.baseApsUrl) {
                environmentRepository.overrideApsEnvironment(ensureTrailingSlash(typedApsUrl.value ?: ""))
                restart = true
            }
        }

        if (authUrlOperationState.value == FieldServiceOperationState.Success) {
            // Only save changes
            if (ensureTrailingSlash(typedAuthUrl.value ?: "") != environmentRepository.selectedConfig.authEnvironmentConfig.baseAuthUrl) {
                environmentRepository.overrideAuthEnvironment(ensureTrailingSlash(typedAuthUrl.value ?: ""))
                restart = true
            }
        }

        // Added logic to retain the url even test failing to unblock lab testing
        if (typedOsccUrl.value.isNotNullOrEmpty()) {
            // Only save changes
            if (ensureTrailingSlash(typedOsccUrl.value ?: "") != environmentRepository.selectedConfig.osccEnvironmentConfig.baseOsccUrl) {
                environmentRepository.overrideOsccEnvironment(ensureTrailingSlash(typedOsccUrl.value ?: ""))
                restart = true
            }
        }

        if (itemProcessorUrlOperationState.value == FieldServiceOperationState.Success) {
            // Only save changes
            if (ensureTrailingSlash(typedItemProcessorUrl.value ?: "") != environmentRepository.selectedConfig.itemProcessorEnvironmentConfig.baseItemProcessorUrl) {
                environmentRepository.overrideItemProcessorEnvironment(ensureTrailingSlash(typedItemProcessorUrl.value ?: ""))
                restart = true
            }
        }

        // Only save changes
        if (ensureTrailingSlash(typedConfigUrl.value ?: "") != environmentRepository.selectedConfig.configEnvironmentConfig.baseConfigUrl) {
            environmentRepository.overrideConfigEnvironment(ensureTrailingSlash(typedConfigUrl.value ?: ""))
            restart = true
        }

        // If changed restart app.
        if (restart) {
            Timber.d("[saveBaseUrls] Restarting app")
            sleep(100)
            ProcessPhoenix.triggerRebirth(app)
        }
    }

    val picassoLoadImageCallback = object : Callback {
        override fun onSuccess() {
            Timber.v("[picassoLoadImageCallback onSuccess] imageUrl=${displayedImageUrl.value}")
            imageUrlOperationState.postValue(FieldServiceOperationState.Success)
            imageUrlLoadInProgress.postValue(false)
        }

        override fun onError(exception: Exception?) {
            Timber.w(exception, "[picassoLoadImageCallback onError] imageUrl=${displayedImageUrl.value}")
            imageUrlOperationState.postValue(FieldServiceOperationState.Failure)
            imageUrlLoadInProgress.postValue(false)
        }
    }
}

enum class FieldServiceOperationState {
    Unknown,
    Success,
    Failure
}

private const val DEFAULT_IMAGE_URL = "https://images.albertsons-media.com/is/image/ABS/108010222"
private const val healthEndpoint = "api/health"
private const val healthCheckOsccEndpoint = "actuator/health"
private const val healthCheckItemProcessorEndpoint = "actuator/health"
private const val APS_SUCCESS_STATUS = "AcuPick Service Up"
private const val OSCC_SUCCESS_STATUS = "UP"
private const val AUTH_SUCCESS_STATUS = "Auth Service UP"
private const val ITEM_PROCESSOR_SUCCESS_STATUS = "UP"
