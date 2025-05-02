package com.albertsons.acupick.wifi.scanner

import com.albertsons.acupick.wifi.settings.Settings
import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper
import com.albertsons.acupick.wifi.model.WiFiConnection
import com.albertsons.acupick.wifi.model.WiFiData
import com.albertsons.acupick.wifi.permission.PermissionService

internal class Scanner(
    val wiFiManagerWrapper: WiFiManagerWrapper,
    val settings: Settings,
    val permissionService: PermissionService,
    val transformer: Transformer
) : ScannerService {
    private val updateNotifiers: MutableList<UpdateNotifier> = mutableListOf()

    private var wiFiData: WiFiData = WiFiData(listOf(), WiFiConnection())
    private var initialScan: Boolean = false

    lateinit var periodicScan: PeriodicScan
    lateinit var scannerCallback: ScannerCallback
    lateinit var scanResultsReceiver: ScanResultsReceiver

    override fun update() {
        wiFiManagerWrapper.enableWiFi()
        if (permissionService.enabled()) {
            scanResultsReceiver.register()
            wiFiManagerWrapper.startScan()
            if (!initialScan) {
                scannerCallback.onSuccess()
                initialScan = true
            }
        }
        wiFiData = transformer.transformToWiFiData()
        updateNotifiers.forEach { it.update(wiFiData) }
    }

    override fun wiFiData(): WiFiData = wiFiData

    override fun register(updateNotifier: UpdateNotifier): Boolean = updateNotifiers.add(updateNotifier)

    override fun unregister(updateNotifier: UpdateNotifier): Boolean = updateNotifiers.remove(updateNotifier)

    override fun pause() {
        periodicScan.stop()
        scanResultsReceiver.unregister()
    }

    override fun running(): Boolean = periodicScan.running

    override fun resume(): Unit = periodicScan.start()

    override fun resumeWithDelay(): Unit = periodicScan.startWithDelay()

    override fun stop() {
        periodicScan.stop()
        updateNotifiers.clear()
        if (settings.wiFiOffOnExit()) {
            wiFiManagerWrapper.disableWiFi()
        }
        scanResultsReceiver.unregister()
    }

    override fun toggle(): Unit =
        if (periodicScan.running) {
            periodicScan.stop()
        } else {
            periodicScan.start()
        }

    fun registered(): Int = updateNotifiers.size
}
