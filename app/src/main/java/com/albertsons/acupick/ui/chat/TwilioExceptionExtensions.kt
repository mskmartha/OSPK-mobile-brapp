package com.albertsons.acupick.ui.chat

import com.albertsons.acupick.data.model.chat.ConversationsError
import com.albertsons.acupick.data.model.chat.toErrorInfo
import com.twilio.util.TwilioException

fun TwilioException.toConversationsError(): ConversationsError =
    ConversationsError.fromErrorInfo(errorInfo)

fun createTwilioException(error: ConversationsError): TwilioException =
    TwilioException(error.toErrorInfo())
