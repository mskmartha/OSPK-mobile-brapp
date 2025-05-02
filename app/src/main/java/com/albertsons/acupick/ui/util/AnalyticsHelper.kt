package com.albertsons.acupick.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import com.albertsons.acupick.data.model.ScannedPickItem
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.DiagnosticsUtils
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.wifi.model.WiFiDetail
import com.albertsons.acupick.wifi.model.getMacAddr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class AnalyticsHelper(private val sharedPreferences: SharedPreferences) : KoinComponent {
    // DI
    private val context: Context by inject()
    private val acuPickLogger: AcuPickLoggerInterface by inject()
    private val userRepository: UserRepository by inject()
    private var lastScannedItem: ScannedPickItem? = null

    val deviceId: String
        @SuppressLint("HardwareIds")
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    object EventKey {
        const val DEVICE_ID = "Device ID"
        const val USER_ID = "User ID"
        const val STORE_ID = "Store ID"
        const val ERROR_EVENT = "Error Event"
        const val ERROR_EVENT_SOURCE = "Error Event Source"
        const val ERROR_TYPE = "Error Type"
        const val ERROR_EVENT_MESSAGE = "Error Event Message"
        const val HTTP_ERROR_CODE = "Http Error Code"
        const val SERVER_ERROR_CODE = "Server Error Code"
        const val LAST_SCANNED_ITEM_ID = "Last Scanned Item ID"
        const val LAST_SCANNED_LOCATION = "Last Scanned Location"
        const val CPU_INFO = "CPU Info"
        const val AVAILABLE_MEMORY = "Available Memory"
        const val TOTAL_MEMORY = "Total Memory"
        // const val WIFI_CLOSEST_ACCESS_POINT = "WiFi Closest Access Point"
        const val WIFI_SSID = "WiFi SSID"
        const val WIFI_BSSID = "WiFi BSSID"
        const val WIFI_VENDOR_NAME = "WiFi Vendor Name"
        const val WIFI_LEVEL = "WiFi Level"
        const val WIFI_BAND = "WiFi Band"
        const val WIFI_WIDTH = "WiFi Width"
    }

    object ErrorType {
        const val AUTH_ERROR = "Auth Error"
        const val GENERIC_ERROR = "Generic Error"
        const val NETWORK_ERROR = "Network Error"
        const val SERVER_ERROR = "Server Error"
        const val FORCE_RESTART = "Force Restart"
    }

    object NetworkErrorType {
        const val NOT_CONNECTED_ERROR = "Not Connected Error"
        const val TIMEOUT_ERROR = "Timeout Error"
        const val VPN_ERROR = "VPN Error"
    }

    private val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        CoroutineScope(Dispatchers.IO).launch {
            userRepository.isLoggedIn.collect {
                if (it) {
                    Timber.d("Persisted force restart sent")
                    sendPersistedErrorEvent(ErrorType.FORCE_RESTART)
                }
            }
        }
    }

    /**
     * Send error event for analytics.
     */
    fun sendErrorEvent(
        eventSource: String? = null,
        errorType: String? = null,
        errorMessage: String? = null,
        httpErrorCode: Int? = null,
        serverErrorCode: Int? = null,
    ) {
        // Set user data for that will be tagged onto error event
        eventSource?.let { source ->
            acuPickLogger.setUserData(EventKey.ERROR_EVENT_SOURCE, source)
        }
        errorType?.let { type ->
            acuPickLogger.setUserData(EventKey.ERROR_TYPE, type)
        }
        errorMessage?.let { message ->
            acuPickLogger.setUserData(EventKey.ERROR_EVENT_MESSAGE, message)
        }
        httpErrorCode?.let { errorCode ->
            acuPickLogger.setUserData(EventKey.HTTP_ERROR_CODE, errorCode)
        }
        serverErrorCode?.let { errorCode ->
            acuPickLogger.setUserData(EventKey.SERVER_ERROR_CODE, errorCode)
        }

        // Ensure user data is provided for error events.
        ensureUserDataIsProvided()

        // Send warning error event to AppDynamics
        acuPickLogger.w(EventKey.ERROR_EVENT)

        // Report metric for counting error events
        var metricValue: String = errorType ?: ""
        if (errorType == ErrorType.NETWORK_ERROR && errorMessage?.isNotNullOrEmpty() == true) {
            metricValue = errorMessage
        }
        val userInfo = userRepository.user.value
        val selectedStoreId = userInfo?.selectedStoreId
        if (selectedStoreId.isNotNullOrEmpty()) {
            metricValue += " StoreId $selectedStoreId"
        }
        acuPickLogger.reportMetric(metricValue, METRIC_COUNT) // e.g. "Timeout Error StoreId 1234"

        // Clear existing error event user data
        clearErrorEvent()
    }

    /**
     * Clear user data from error event.
     */
    fun clearErrorEvent() {
        acuPickLogger.setUserData(EventKey.ERROR_EVENT_SOURCE, "")
        acuPickLogger.setUserData(EventKey.ERROR_TYPE, "")
        acuPickLogger.setUserData(EventKey.ERROR_EVENT_MESSAGE, "")
        acuPickLogger.setUserData(EventKey.HTTP_ERROR_CODE, "")
        acuPickLogger.setUserData(EventKey.SERVER_ERROR_CODE, "")
        acuPickLogger.setUserData(EventKey.LAST_SCANNED_ITEM_ID, "")
        acuPickLogger.setUserData(EventKey.LAST_SCANNED_LOCATION, "")
    }

    /**
     * Persist restart event for sending analytics on restart.
     */
    fun persistErrorEvent(errorType: String, eventSource: String?) {
        sharedPreferences.edit {
            putString(errorType, eventSource)
        }
    }

    /**
     * Send persisted restart event after the restarts.
     */
    fun sendPersistedErrorEvent(errorType: String) {
        val eventSource = sharedPreferences.getString(errorType, null)
        eventSource?.let {
            try {
                throw Error(errorType)
            } catch (e: Error) {
                sendErrorEvent(
                    eventSource = eventSource,
                    errorType = errorType
                )
                sharedPreferences.edit {
                    remove(errorType)
                }
            }
        }
    }

    /**
     * Ensure user data is provided for error events.
     */
    private fun ensureUserDataIsProvided() {
        // Correlation ID
        val uuid = UUID.randomUUID().toString()
        acuPickLogger.setUserData("LastHttpUuid", uuid)

        wifiManager.connectionInfo?.let { wifiInfo ->
            acuPickLogger.setUserData("RSSI", wifiInfo.rssi)
            acuPickLogger.setUserData("SSID", wifiInfo.ssid)
            acuPickLogger.setUserData("BSSID", wifiInfo.bssid)
            val ipAddress = (
                ((wifiInfo.ipAddress and 0xff).toString() + "." + (wifiInfo.ipAddress shr 8 and 0xff)).toString() +
                    "." + (wifiInfo.ipAddress shr 16 and 0xff).toString()
                ) + "." + (wifiInfo.ipAddress shr 24 and 0xff)
            acuPickLogger.setUserData("IP_ADDRESS", ipAddress)
            acuPickLogger.setUserData("MAC_ADDRESS", getMacAddr())
        }

        // User ID and Store ID
        val userInfo = userRepository.user.value
        acuPickLogger.setUserData(EventKey.USER_ID, userInfo?.userId)
        when {
            userInfo?.sites?.isEmpty() == true -> {
                acuPickLogger.setUserData(EventKey.STORE_ID, "")
            }
            userInfo?.selectedStoreId.isNotNullOrEmpty() -> {
                acuPickLogger.setUserData(EventKey.STORE_ID, userInfo?.selectedStoreId)
            }
            userInfo?.sites?.size == 1 -> {
                acuPickLogger.setUserData(EventKey.STORE_ID, userInfo.sites.firstOrNull() ?: "")
            }
        }

        // Device ID
        acuPickLogger.setUserData(EventKey.DEVICE_ID, getSerialNumber())

        // Last scanned item and location
        lastScannedItem?.item?.itemAddressDto?.let {
            val itemId = lastScannedItem?.item?.itemId ?: ""
            val lastPickLocation = "aisle=${it.aisleSeq}, bay=${it.bay}, dept=${it.deptShortName}, level=${it.level}, side=${it.side}"
            acuPickLogger.setUserData(EventKey.LAST_SCANNED_ITEM_ID, itemId)
            acuPickLogger.setUserData(EventKey.LAST_SCANNED_LOCATION, lastPickLocation)
        }

        // CPU Info
        acuPickLogger.setUserData(EventKey.CPU_INFO, getCpuUsageInfo())

        // Available Memory and Total Memory
        val memoryUsage = getMemoryUsage()

        val bdAvailableMemory = BigDecimal(memoryUsage.first)
        val availableMemory = bdAvailableMemory.setScale(2, RoundingMode.FLOOR)

        val bdTotalMemory = BigDecimal(memoryUsage.second)
        val totalMemory = bdTotalMemory.setScale(2, RoundingMode.FLOOR)

        acuPickLogger.setUserData(EventKey.AVAILABLE_MEMORY, "$availableMemory GB")
        acuPickLogger.setUserData(EventKey.TOTAL_MEMORY, "$totalMemory GB")
    }

    /**
     * Diagnostics info for analytics.
     */
    private fun getCpuUsageInfo(): String {
        val cores = DiagnosticsUtils.coresUsageGuessFromFreq
        return DiagnosticsUtils.getCpuUsage(cores).toString() + "%"
    }

    private fun getMemoryUsage(): Pair<Double, Double> {
        return DiagnosticsUtils.getMemoryUsage(context)
    }

    /**
     * Send closest WiFi access point for analytics.
     */
    fun sendWiFiClosestAccessPointEvent(
        wiFiDetails: WiFiDetail
    ) {
        val ssid = wiFiDetails.wiFiIdentifier.ssid
        val bssid = wiFiDetails.wiFiIdentifier.bssid
        val vendorName = wiFiDetails.wiFiAdditional.vendorName
        val level = wiFiDetails.wiFiSignal.level
        val wiFiBand = wiFiDetails.wiFiSignal.wiFiBand.name
        val wiFiWidth = wiFiDetails.wiFiSignal.wiFiWidth.name

        acuPickLogger.setUserData(EventKey.WIFI_SSID, ssid)
        acuPickLogger.setUserData(EventKey.WIFI_BSSID, bssid)
        acuPickLogger.setUserData(EventKey.WIFI_VENDOR_NAME, vendorName)
        acuPickLogger.setUserData(EventKey.WIFI_LEVEL, level)
        acuPickLogger.setUserData(EventKey.WIFI_BAND, wiFiBand)
        acuPickLogger.setUserData(EventKey.WIFI_WIDTH, wiFiWidth)

        // acuPickLogger.w(EventKey.WIFI_CLOSEST_ACCESS_POINT)
        clearWiFiClosestAccessPointEvent()
    }

    /**
     * Clear user data from WiFi closest access point event.
     */
    fun clearWiFiClosestAccessPointEvent() {
        acuPickLogger.setUserData(EventKey.WIFI_SSID, "")
        acuPickLogger.setUserData(EventKey.WIFI_BSSID, "")
        acuPickLogger.setUserData(EventKey.WIFI_VENDOR_NAME, "")
        acuPickLogger.setUserData(EventKey.WIFI_LEVEL, "")
        acuPickLogger.setUserData(EventKey.WIFI_BAND, "")
        acuPickLogger.setUserData(EventKey.WIFI_WIDTH, "")
    }

    @SuppressLint("HardwareIds")
    @SuppressWarnings("unchecked", "deprecation")
    fun getSerialNumber(): String? {
        var serialNumber: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            serialNumber = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)
        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            serialNumber = Build.getSerial()
        }
        return serialNumber
    }

    /**
     * Manage last scanned item for analytics purposes.
     */
    fun setLastScannedItem(scannedItem: ScannedPickItem?) {
        lastScannedItem = scannedItem
    }

    fun scannedItemNoLongerRelevent() {
        lastScannedItem = null
    }

    companion object {
        const val METRIC_COUNT: Long = 1
    }
}
