package com.albertsons.acupick.data.model

import com.squareup.moshi.JsonClass

data class CredentialModel(val id: String?, val password: String?) {
    fun isIdValid(): Boolean {
        // TODO: add any additional validation rules here
        return !id.isNullOrBlank()
    }

    fun isPasswordValid(): Boolean {
        // TODO: add any additional validation rules here
        return !password.isNullOrBlank()
    }

    val valid: Boolean
        get() = isIdValid() && isPasswordValid()

    val validCredentials: ValidCredentialModel?
        get() = if (valid) ValidCredentialModel(id!!, password!!) else null
}

@JsonClass(generateAdapter = true)
data class ValidCredentialModel(val id: String, val password: String)
