package com.albertsons.acupick.ui.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.albertsons.acupick.data.model.DomainModel
import java.io.Serializable

/**
 * Union type that represents either the int ID or the raw String.
 *
 * Calling getString resolves to a String from either type.
 */
sealed class StringIdHelper : DomainModel, Serializable {
    data class Id(@StringRes val idRes: Int) : StringIdHelper()

    data class Raw(val rawString: String) : StringIdHelper()

    data class Format(@StringRes val idRes: Int, val rawString: String, val additionalString: String? = null) : StringIdHelper()

    data class Plural(@PluralsRes val idRes: Int, val quantity: Int) : StringIdHelper()

    data class FormatWithExtraAdditionalString(@StringRes val idRes: Int, val rawString: String, val additionalString: String, val extraAdditionalString: String) : StringIdHelper()

    fun getString(context: Context): String {
        return when (this) {
            is Id -> {
                context.getString(idRes)
            }
            is Raw -> {
                rawString
            }
            is Format -> {
                if (additionalString == null) context.getString(idRes, rawString) else context.getString(idRes, rawString, additionalString)
            }
            is Plural -> {
                context.resources.getQuantityString(idRes, quantity, quantity)
            }
            is FormatWithExtraAdditionalString -> {
                context.getString(idRes, rawString, additionalString, extraAdditionalString)
            }
        }
    }
}

fun String.toRawHelper() = StringIdHelper.Raw(this)

fun Int.toIdHelper() = StringIdHelper.Id(this)

fun Int.toFormatHelper(rawString: String, additionalString: String? = null) =
    StringIdHelper.Format(this, rawString, additionalString)
