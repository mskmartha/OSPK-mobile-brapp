package com.albertsons.acupick.data.network

import com.albertsons.acupick.data.model.response.ServerErrorCodeDto
import com.albertsons.acupick.data.model.response.ServerErrorCode
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class ServerErrorCodeAdapter {
    @ToJson
    fun toJson(value: ServerErrorCodeDto): Int {
        return value.rawValue
    }

    @FromJson
    fun fromJson(value: Int): ServerErrorCodeDto {
        return ServerErrorCodeDto(rawValue = value, resolvedType = ServerErrorCode.values().firstOrNull { it.value == value } ?: ServerErrorCode.UNKNOWN_SERVER_ERROR_CODE)
    }
}
