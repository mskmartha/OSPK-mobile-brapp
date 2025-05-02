package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import com.albertsons.acupick.data.logic.HandOffVerificationState
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.models.VerificationInfo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.ZonedDateTime

class HandOffVerificationSharedViewModel(
    app: Application,
) : BaseViewModel(app) {

    val idUnavailableEvent = MutableSharedFlow<String?>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pickupPersonDataCompleteEvent = MutableSharedFlow<String?>(replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val orderInfoMap = hashMapOf<String, VerificationInfo>()

    val handOffVerificationStateMap = hashMapOf<String, HandOffVerificationState>()
    val handOffResultDataMap = hashMapOf<String, HandOffResultData?>()
    val authCodeIssueReportedMap = hashMapOf<String, Boolean>()
    val authCodeVerifiedMap = hashMapOf<String, Boolean>()
    val checkMarkCheckedMap = hashMapOf<String, Boolean>()
    val idVerifiedMap = hashMapOf<String, Boolean>()
    val itemsRemoved = hashMapOf<String, Boolean>()
    val removeItemsCtaEnabledMap = hashMapOf<String, Boolean>()
    val otpCapturedTimestampMap = hashMapOf<String, ZonedDateTime?>()
    val otpBypassTimestampMap = hashMapOf<String, ZonedDateTime?>()
    val authCodeUnavailableReasonCodeMap = hashMapOf<String, AuthCodeUnavailableReasonCode?>()
}
