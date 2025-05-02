package com.albertsons.acupick.ui.util

import android.content.Context
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.SubstitutionCode

fun SubstitutionCode?.fetchSubstitutionString(context: Context) =
    when (this) {
        SubstitutionCode.SAME_SIZE_DIFF_BRAND -> context.getString(R.string.same_size_diff_brand)
        SubstitutionCode.SAME_BRAND_DIFF_SIZE -> context.getString(R.string.same_brand_diff_size)
        SubstitutionCode.USE_SUGGESTED_SUB -> ""
        SubstitutionCode.ONLY_USE_SUGGESTED_SUB -> context.getString(R.string.only_use_suggested_substitution)
        SubstitutionCode.NOT_ALLOWED, null -> context.getString(R.string.not_allowed)
        // We should never end up actually using this (not allowed string) as subAllowed boolean will gate us, but just in case.
    }

fun SubstitutionCode?.fetchSuggestedItemHeaderString(context: Context) = context.getString(
    when (this) {
        SubstitutionCode.ONLY_USE_SUGGESTED_SUB -> R.string.substitute_suggested_header_customer_chosen
        else -> R.string.substitute_suggested_header
    }
)
