package com.albertsons.acupick.wifi.scanner

import android.app.Activity
import android.os.Handler
import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import com.albertsons.acupick.wifi.model.WiFiData
import com.albertsons.acupick.wifi.permission.PermissionService

interface UpdateNotifier {
    fun update(wiFiData: WiFiData)
}

interface ScannerService {
    fun update()
    fun wiFiData(): WiFiData
    fun register(updateNotifier: UpdateNotifier): Boolean
    fun unregister(updateNotifier: UpdateNotifier): Boolean
    fun pause()
    fun running(): Boolean
    fun resume()
    fun resumeWithDelay()
    fun stop()
    fun toggle()
}

fun makeScannerService(
    activity: Activity,
    wiFiManagerWrapper: WiFiManagerWrapper,
    handler: Handler,
    settings: Settings
): ScannerService {
    val cache = Cache()
    val transformer = Transformer(cache)
    val permissionService = PermissionService(activity)
    val scanner = Scanner(wiFiManagerWrapper, settings, permissionService, transformer)
    scanner.periodicScan = PeriodicScan(scanner, handler, settings)
    scanner.scannerCallback = ScannerCallback(wiFiManagerWrapper, cache)
    scanner.scanResultsReceiver = ScanResultsReceiver(activity, scanner.scannerCallback)
    return scanner
}
