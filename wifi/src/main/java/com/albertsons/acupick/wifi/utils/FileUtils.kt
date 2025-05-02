package com.albertsons.acupick.wifi.utils

import android.content.res.Resources
import androidx.annotation.RawRes
import java.io.InputStream

fun readFile(resources: Resources, @RawRes id: Int): String {
    return try {
        resources.openRawResource(id).use { read(it) }
    } catch (e: Exception) {
        ""
    }
}

private fun read(inputStream: InputStream): String {
    val size = inputStream.available()
    val bytes = ByteArray(size)
    val count = inputStream.read(bytes)
    return if (count == size) String(bytes).replace("\r", "") else ""
}
