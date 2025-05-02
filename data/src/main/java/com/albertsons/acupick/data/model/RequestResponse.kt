package com.albertsons.acupick.data.model

/** Encapsulates a request/response combination (syntax sugar over using a Pair with better property names instead of using .first and .last) */
data class RequestResponse<REQUEST, RESPONSE>(val request: REQUEST, val response: RESPONSE)
