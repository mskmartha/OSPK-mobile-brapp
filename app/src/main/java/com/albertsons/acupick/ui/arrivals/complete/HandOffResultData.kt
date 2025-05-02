package com.albertsons.acupick.ui.arrivals.complete

import android.os.Parcelable
import androidx.annotation.Keep
import com.albertsons.acupick.data.model.AuthCodeUnavailableReasonCode
import com.albertsons.acupick.data.model.CancelReasonCode
import com.albertsons.acupick.data.model.request.PickUpUserRequestDto
import com.albertsons.acupick.data.model.request.RxOrder
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

@Parcelize
@Keep
data class HandOffResultData(
    val isCancel: Boolean = false,
    val markedCompleted: Boolean = false,
    val cancelReasonCode: CancelReasonCode? = null,
    val authCodeUnavailableReasonCode: AuthCodeUnavailableReasonCode? = null,
    val userInputAuthCode: String? = null,
    val completeOrCancelTime: ZonedDateTime,
    var isIdVerified: Boolean,
    val otpCapturedTimestamp: ZonedDateTime? = null,
    val otpByPassTimeStamp: ZonedDateTime? = null,
    val rxOrders: List<RxOrder>? = null,
    val pickupUserInfoReq: PickUpUserRequestDto? = null
) : Parcelable
