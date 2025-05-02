package com.albertsons.acupick.data.network.logging

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.albertsons.acupick.data.environment.AuthEnvironmentType
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.isNotNullOrBlank
import com.albertsons.acupick.wifi.model.getIpAddress
import com.albertsons.acupick.wifi.model.getMacAddr
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class HeaderInterceptor(
    private val loggingDataProvider: LoggingDataProvider,
    private val acuPickLogger: AcuPickLoggerInterface,
    private val environmentRepository: EnvironmentRepository,
    context: Context,
) : Interceptor, KoinComponent {

    private val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val userRepository: UserRepository by inject()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val correlationId = UUID.randomUUID().toString()

        // User Data deemed for deprecation in upcoming release
        acuPickLogger.setUserData("LastHttpUuid", correlationId)
        // User Data via ABS Observability Standards v1.0
        acuPickLogger.setUserData("CorrelationId", correlationId)

        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo?.isConnected == true) {
            acuPickLogger.setUserData("SSID", networkInfo.extraInfo?.trim('\"'))
        }

        val macAddress = getMacAddr()
        val ipAddress = getIpAddress(wifiManager)
        wifiManager.connectionInfo?.let { wifiInfo ->
            acuPickLogger.setUserData("RSSI", wifiInfo.rssi)
            acuPickLogger.setUserData("BSSID", wifiInfo.bssid)
            acuPickLogger.setUserData("IP_ADDRESS", ipAddress)
            acuPickLogger.setUserData("MAC_ADDRESS", macAddress)
        }

        val newRequest = request.newBuilder()
            // Headers deemed for deprecation in future release
            .addHeader(HEADER_DEPRECATED_APP_VERSION, loggingDataProvider.appVersion)
            .addHeader(HEADER_DEPRECATED_CLIENT_PLATFORM, HEADER_DEPRECATED_CLIENT_PLATFORM_PICKER)
            .addHeader(HEADER_DEPRECATED_DEVICE_ID, loggingDataProvider.deviceId)
            .addHeader(HEADER_DEPRECATED_STORE_ID, loggingDataProvider.storeId)
            .addHeader(HEADER_DEPRECATED_UUID, correlationId)
            // Headers via ABS Observability Standards v1.0
            .addHeader(HEADER_TELEMETRY_VERSION, "1.0")
            .addHeader(HEADER_CORRELATION_ID, correlationId)
            .addHeader(HEADER_APP_CODE, "AcuPick")
            .addHeader(HEADER_APP_VERSION, loggingDataProvider.appVersion)
            .addHeader(HEADER_CLIENT_PLATFORM, "android-picker-app")
            .addHeader(HEADER_CLIENT_DEVICE_ID, loggingDataProvider.deviceId)
            .addHeader(HEADER_MAC_ADDRESS, macAddress)
            .addHeader(HEADER_IP_ADDRESS, ipAddress)

        if (loggingDataProvider.storeId.isNotNullOrBlank()) {
            newRequest.addHeader(HEADER_LOCATION_ID, loggingDataProvider.storeId)
        }

        userRepository.user.value?.userId?.let { userId ->
            newRequest.addHeader(HEADER_USER_ID, userId)
        }

        userRepository.user.value?.userActivityId?.let { userActivityId ->
            newRequest.addHeader(HEADER_USER_ACTIVITY_ID, userActivityId.toString())
        }

        // Note: The following code block is commented out as it is not needed for the current implementation
        // val selectedEnvConfig = environmentRepository.selectedConfig.apsEnvironmentConfig
        // val isApsRequest = request.url.toString().contains(selectedEnvConfig.baseApsUrl)
        // if (isApsRequest) {
        //     when (selectedEnvConfig.apsEnvironmentType) {
        //         // ApsEnvironmentType.CANARY -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_CANARY_SUBSCRIPTION_KEY_VALUE)
        //         // ApsEnvironmentType.PRODUCTION_CANARY -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_PROD_CANARY_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_QA1 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_QA1_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_QA2 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_QA2_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_QA3 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_QA3_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_QA4 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_QA4_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_QA5 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_QA5_SUBSCRIPTION_KEY_VALUE)
        //         ApsEnvironmentType.APIM_PERF -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_SERVICES_PERF_SUBSCRIPTION_KEY_VALUE)
        //         else -> {}
        //     }
        // }

        // Note: The following code block is commented out as it is not needed for the current implementation
        // sval itemProcessorSelectedEnvConfig = environmentRepository.selectedConfig.itemProcessorEnvironmentConfig
        // val isItemProcessorRequest = request.url.toString().contains(itemProcessorSelectedEnvConfig.baseItemProcessorUrl)
        // if (isItemProcessorRequest) {
        //     when (itemProcessorSelectedEnvConfig.itemProcessorEnvironmentType) {
        //         // ItemProcessorEnvironmentType.CANARY -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_CANARY_SUBSCRIPTION_KEY_VALUE)
        //         // ItemProcessorEnvironmentType.PRODUCTION_CANARY -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_PROD_CANARY_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_QA1 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_QA1_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_QA2 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_QA2_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_QA3 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_QA3_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_QA4 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_QA4_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_QA5 -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_QA5_SUBSCRIPTION_KEY_VALUE)
        //         ItemProcessorEnvironmentType.APIM_PERF -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_OSPK_ITEM_PROCESSOR_PERF_SUBSCRIPTION_KEY_VALUE)
        //         // ItemProcessorEnvironmentType.ITEM_PROCESSOR_BASE_APS_URL -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_QA1_SUBSCRIPTION_KEY_VALUE)
        //         else -> {}
        //     }
        // }

        val isAuthRequest = request.url.toString().contains(environmentRepository.selectedConfig.authEnvironmentConfig.baseAuthUrl)
        if (isAuthRequest) {
            when (environmentRepository.selectedConfig.authEnvironmentConfig.authEnvironmentType) {
                AuthEnvironmentType.PRODUCTION_CANARY -> newRequest.addHeader(HEADER_CANARY_SUBSCRIPTION_KEY, HEADER_PROD_CANARY_SUBSCRIPTION_KEY_VALUE)
                else -> {}
            }
        }
        return chain.proceed(newRequest.build())
    }

    companion object {
        // Headers deemed for deprecation in future release
        const val HEADER_DEPRECATED_APP_VERSION = "AppVersion"
        const val HEADER_DEPRECATED_CLIENT_PLATFORM = "ClientPlatform"
        const val HEADER_DEPRECATED_CLIENT_PLATFORM_PICKER = "android-picker-app"
        const val HEADER_DEPRECATED_DEVICE_ID = "DeviceId"
        const val HEADER_DEPRECATED_STORE_ID = "StoreId"
        const val HEADER_DEPRECATED_UUID = "UUID"

        // Headers via ABS Observability Standards v1.0
        const val HEADER_TELEMETRY_VERSION = "x-abs-telemetry-version"
        const val HEADER_CORRELATION_ID = "x-abs-correlation-id"
        const val HEADER_USER_ID = "x-abs-user-id"
        const val HEADER_USER_ACTIVITY_ID = "x-abs-userActivityId"
        const val HEADER_LOCATION_ID = "x-abs-location-id"
        const val HEADER_APP_CODE = "x-abs-app-code"
        const val HEADER_MAC_ADDRESS = "x-abs-mac-address"
        const val HEADER_IP_ADDRESS = "x-abs-ip-address"
        const val HEADER_APP_VERSION = "x-abs-app-version"
        const val HEADER_CLIENT_PLATFORM = "x-abs-client-platform"
        const val HEADER_CLIENT_DEVICE_ID = "x-abs-client-device-id"
        const val HEADER_CANARY_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key"
        // const val HEADER_CANARY_SUBSCRIPTION_KEY_VALUE = "5ca4ba69e54f4f4dafe0f821333cb5bd"
        const val HEADER_PROD_CANARY_SUBSCRIPTION_KEY_VALUE = "d474511417a643069df8e687db9e9ac4"
        // // Pick Service Subscription Keys
        // const val HEADER_OSPK_SERVICES_QA1_SUBSCRIPTION_KEY_VALUE = "add2ae7ee2b14e41ad459712abde784d"
        // const val HEADER_OSPK_SERVICES_QA2_SUBSCRIPTION_KEY_VALUE = "7fbe1b44cedb4ce1b833eb677440052a"
        // const val HEADER_OSPK_SERVICES_QA3_SUBSCRIPTION_KEY_VALUE = "5add638947e04839ba9dcbb9d99d37a8"
        // const val HEADER_OSPK_SERVICES_QA4_SUBSCRIPTION_KEY_VALUE = "f96f56cf23ad4678a583f1b70bf596df"
        // const val HEADER_OSPK_SERVICES_QA5_SUBSCRIPTION_KEY_VALUE = "76aa83b113b84c09a615004c5b3ab742"
        // const val HEADER_OSPK_SERVICES_PERF_SUBSCRIPTION_KEY_VALUE = "cd8fcfa248674be992db2696156c1c17"
        //
        // // Item Processor Subcription Keys
        // const val HEADER_OSPK_ITEM_PROCESSOR_QA1_SUBSCRIPTION_KEY_VALUE = "23478e962b5f4ca98cd644257a3507f1"
        // const val HEADER_OSPK_ITEM_PROCESSOR_QA2_SUBSCRIPTION_KEY_VALUE = "f332d1594374431cab0dde59e880cc0f"
        // const val HEADER_OSPK_ITEM_PROCESSOR_QA3_SUBSCRIPTION_KEY_VALUE = "249a4a14ca0846dc957f9a74ac5f2250"
        // const val HEADER_OSPK_ITEM_PROCESSOR_QA4_SUBSCRIPTION_KEY_VALUE = "198928662a044dd58767d58e63dd53c6"
        // const val HEADER_OSPK_ITEM_PROCESSOR_QA5_SUBSCRIPTION_KEY_VALUE = "7947bd26f5cf4fcd90be10d9e311114f"
        // const val HEADER_OSPK_ITEM_PROCESSOR_PERF_SUBSCRIPTION_KEY_VALUE = "5bdca5d2558a4180a0cc420fa0aaf63d"
    }
}
