package com.albertsons.acupick.data.model.chat

import com.twilio.conversations.Participant

data class ParticipantDataItem(
    val sid: String,
    val identity: String,
    val conversationSid: String,
)

fun Participant.asParticipantDataItem() = ParticipantDataItem(
    sid = this.sid,
    conversationSid = this.conversation.sid,
    identity = this.identity,
)
