package com.albertsons.acupick.data.network.logging

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.albertsons.acupick.data.environment.EnvironmentRepository
import com.albertsons.acupick.data.environment.OsccEnvironmentType
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

class OSCCHeaderInterceptor(
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
            .addHeader(HEADER_APP_VERSION, loggingDataProvider.appVersion)
            .addHeader(HEADER_CLIENT_DEVICE_ID, loggingDataProvider.deviceId)
            .addHeader(HEADER_MAC_ADDRESS, macAddress)
            .addHeader(HEADER_IP_ADDRESS, ipAddress)
            .addHeader(HEADER_TELEMETRY_VERSION, "1.0")
            .addHeader(HEADER_CORRELATION_ID, correlationId)
            .addHeader(HEADER_APP_CODE, "AcuPick")
            .addHeader(HEADER_CLIENT_PLATFORM, "android-picker-app")

        if (loggingDataProvider.storeId.isNotNullOrBlank()) {
            newRequest.addHeader(HEADER_LOCATION_ID, loggingDataProvider.storeId)
        }

        userRepository.user.value?.userId?.let { userId ->
            newRequest.addHeader(HEADER_USER_ID, userId)
        }

        val selectedEnvConfig = environmentRepository.selectedConfig.osccEnvironmentConfig
        when (selectedEnvConfig.osccEnvironmentType) {
            OsccEnvironmentType.QA -> newRequest.addHeader(HEADER_OSCC_SUBSCRIPTION_KEY, HEADER_QA_OSCC_SUBSCRIPTION_KEY_VALUE)
            OsccEnvironmentType.QA2 -> newRequest.addHeader(HEADER_OSCC_SUBSCRIPTION_KEY, HEADER_QA3_OSCC_SUBSCRIPTION_KEY_VALUE)
            OsccEnvironmentType.PRODUCTION -> newRequest.addHeader(HEADER_OSCC_SUBSCRIPTION_KEY, HEADER_PROD_OSCC_SUBSCRIPTION_KEY_VALUE)
        }
        return chain.proceed(newRequest.build())
    }

    companion object {
        const val HEADER_USER_ID = "x-abs-user-id"
        const val HEADER_LOCATION_ID = "x-abs-location-id"
        const val HEADER_APP_VERSION = "x-abs-app-version"
        const val HEADER_CLIENT_DEVICE_ID = "x-abs-client-device-id"
        const val HEADER_OSCC_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key"
        const val HEADER_MAC_ADDRESS = "x-abs-mac-address"
        const val HEADER_IP_ADDRESS = "x-abs-ip-address"
        const val HEADER_TELEMETRY_VERSION = "x-abs-telemetry-version"
        const val HEADER_CORRELATION_ID = "x-abs-correlation-id"
        const val HEADER_APP_CODE = "x-abs-app-code"
        const val HEADER_CLIENT_PLATFORM = "x-abs-client-platform"

        const val HEADER_QA_OSCC_SUBSCRIPTION_KEY_VALUE = "e10a39d5dfd4442aa07b2cc3aad79fd2"
        const val HEADER_QA3_OSCC_SUBSCRIPTION_KEY_VALUE = "43df018ed27a4bad87bb0f000f734b0d"
        const val HEADER_PROD_OSCC_SUBSCRIPTION_KEY_VALUE = "f52a626ce3c34c29a1e6760614d93814"
    }
}
