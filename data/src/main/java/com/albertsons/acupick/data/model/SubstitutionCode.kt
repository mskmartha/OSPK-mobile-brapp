package com.albertsons.acupick.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

enum class SubstitutionCode {
    NOT_ALLOWED,
    SAME_SIZE_DIFF_BRAND,
    SAME_BRAND_DIFF_SIZE,
    USE_SUGGESTED_SUB,
    ONLY_USE_SUGGESTED_SUB,
}

fun SubstitutionCode?.isNotCustomerSuggestedSubCode(): Boolean =
    when (this) {
        SubstitutionCode.ONLY_USE_SUGGESTED_SUB -> false
        else -> true
    }

class SubstitutionCodeAdapter {
    @ToJson
    fun toJson(value: SubstitutionCode?): String? {
        return when (value) {
            SubstitutionCode.NOT_ALLOWED -> "0"
            SubstitutionCode.SAME_SIZE_DIFF_BRAND -> "1"
            SubstitutionCode.SAME_BRAND_DIFF_SIZE -> "2"
            SubstitutionCode.USE_SUGGESTED_SUB -> "3"
            SubstitutionCode.ONLY_USE_SUGGESTED_SUB -> "4"
            null -> null
        }
    }

    @FromJson
    fun fromJson(value: String): SubstitutionCode? {
        return when (value.trimStart('0')) {
            // Zero is for documentation, as it will get trimmed away.
            "0", "" -> SubstitutionCode.NOT_ALLOWED
            "1" -> SubstitutionCode.SAME_SIZE_DIFF_BRAND
            "2" -> SubstitutionCode.SAME_BRAND_DIFF_SIZE
            "3" -> SubstitutionCode.USE_SUGGESTED_SUB
            "4" -> SubstitutionCode.ONLY_USE_SUGGESTED_SUB
            else -> null
        }
    }
}
