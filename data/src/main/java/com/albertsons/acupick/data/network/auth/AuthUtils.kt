package com.albertsons.acupick.data.network.auth

import org.apache.commons.codec.binary.Base64

internal fun getBasicAuthHeader(username: String, password: String): String {
    val auth = String(Base64.encodeBase64("$username:$password".toByteArray()))
    return "Basic $auth"
}
