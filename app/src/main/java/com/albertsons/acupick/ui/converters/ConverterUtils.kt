package com.albertsons.acupick.ui.converters

import androidx.databinding.InverseMethod

/*
* Allows for 2-way data binding using Ints in EditText
* https://developer.android.com/topic/libraries/data-binding/two-way#converters
*/
object ConverterUtils {

    fun convertStringToInt(text: String): Int? {
        return text.toIntOrNull()
    }

    @InverseMethod(value = "convertStringToInt")
    fun convertIntToString(value: Int?): String? {
        return value?.toString() ?: ""
    }
}
