package com.albertsons.acupick.data.network.logging

/** Provides values for network request headers from the application module */
interface LoggingDataProvider {

    val appVersion: String
    val deviceId: String
    var storeId: String
}
