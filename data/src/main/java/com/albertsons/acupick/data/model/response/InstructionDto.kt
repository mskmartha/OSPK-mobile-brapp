package com.albertsons.acupick.data.model.response

import android.os.Parcelable
import com.albertsons.acupick.data.model.Dto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize

/**
 * Corresponds to the Instruction swagger api
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class InstructionDto(
    /** **Note: You should probably be using the [InstructionDto.displayText] extension function** Customer instructions (for the particular item). */
    @Json(name = "text") val text: String? = null,
    /** Type is only for future purpose, not used as per the current design. */
    @Json(name = "type") val type: String? = null
) : Parcelable, Dto
