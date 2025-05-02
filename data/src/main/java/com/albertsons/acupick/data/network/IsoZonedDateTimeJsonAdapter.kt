package com.albertsons.acupick.data.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class IsoZonedDateTimeJsonAdapter : JsonAdapter<ZonedDateTime>() {
    companion object {
        // TODO: Find out if there's a better solution than having separate formatters for reading and writing
        private val IN_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME
        private val OUT_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ZonedDateTime?) {
        value?.let {
            writer.value(it.format(OUT_FORMATTER))
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ZonedDateTime? {
        return if (reader.peek() != JsonReader.Token.NULL) {
            val dateTimeString = reader.nextString()
            try {
                ZonedDateTime.parse(dateTimeString, IN_FORMATTER).also {
                    // Timber.v("[fromJson] parsed ZonedDateTime=$it, using system/device timezone=${it.withZoneSameInstant(ZoneId.systemDefault())}")
                }
            } catch (e: DateTimeParseException) {
                Timber.e("[fromJson] error parsing ZonedDateTime for '${reader.path}' with value of '$dateTimeString'- returning null")
                null
            }
        } else {
            reader.nextNull<Any>()
            null
        }
    }
}

class ChatZonedDateTimeJsonAdapter : JsonAdapter<ZonedDateTime>() {
    companion object {
        private val IN_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME
        private val OUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ZonedDateTime?) {
        value?.let {
            writer.value(it.format(OUT_FORMATTER))
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ZonedDateTime? {
        return if (reader.peek() != JsonReader.Token.NULL) {
            val dateTimeString = reader.nextString()
            try {
                ZonedDateTime.parse(dateTimeString, IN_FORMATTER).also {
                    // Timber.v("[fromJson] parsed ZonedDateTime=$it, using system/device timezone=${it.withZoneSameInstant(ZoneId.systemDefault())}")
                }
            } catch (e: DateTimeParseException) {
                Timber.e("[fromJson] error parsing ZonedDateTime for '${reader.path}' with value of '$dateTimeString'- returning null")
                null
            }
        } else {
            reader.nextNull<Any>()
            null
        }
    }
}
