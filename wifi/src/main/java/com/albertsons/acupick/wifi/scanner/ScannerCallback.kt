package com.albertsons.acupick.wifi.scanner

import com.albertsons.acupick.wifi.manager.WiFiManagerWrapper

internal class ScannerCallback(
    private val wiFiManagerWrapper: WiFiManagerWrapper,
    private val cache: Cache
) : Callback {
    override fun onSuccess() {
        cache.add(wiFiManagerWrapper.scanResults(), wiFiManagerWrapper.wiFiInfo())
    }
}
