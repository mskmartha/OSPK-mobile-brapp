package com.albertsons.acupick.ui.bottomsheetdialog

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.BaseViewModel
import com.albertsons.acupick.ui.arrivals.complete.HandOffAuthInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthCodeVerificationViewModel(
    val app: Application,
) : BaseViewModel(app) {

    val isErrorShown = MutableStateFlow(false)

    // The auth code to be verified from the Parent View model
    private val _param = MutableStateFlow<HandOffAuthInfo?>(null)
    val param = _param.asStateFlow()

    private val _focusedIndex = MutableStateFlow<Int?>(null)
    val focusedIndex = _focusedIndex.asStateFlow()

    // The verification code entered by the user
    val verificationCodeText = MutableStateFlow("")

    // trigger Code Unavailable event
    val codeUnavailableEvent = MutableSharedFlow<Unit>()

    // trigger Auth Code Validation Results event
    val authCode = MutableSharedFlow<String?>()

    fun setVerificationCode(code: String) {
        viewModelScope.launch {
            verificationCodeText.emit(code)
        }
    }

    fun setParam(authInfo: HandOffAuthInfo) {
        viewModelScope.launch {
            _param.emit(authInfo)
        }
    }

    fun onClickConfirmAuthCode() {
        viewModelScope.launch {
            authCode.emit(verificationCodeText.value)
            if (param.value?.authCode.isNotNullOrEmpty() && param.value?.authCode != verificationCodeText.value) {
                isErrorShown.emit(true)
            }
        }
    }

    fun onClickCodeUnavailable() {
        viewModelScope.launch {
            codeUnavailableEvent.emit(Unit)
        }
    }

    fun resetFocussedIndex() {
        _focusedIndex.update { 0 }
    }

    fun onKeyboardBack() {
        val previousIndex = getPreviousFocusedIndex()
        val newCode = verificationCodeText.value.mapIndexed { index, number ->
            if (index == previousIndex) {
                ""
            } else {
                number
            }
        }
        verificationCodeText.update { newCode.joinToString("") }
        _focusedIndex.update { previousIndex }
    }

    fun onEnterNumber(number: String, index: Int) {

        if (verificationCodeText.value.getOrNull(index)?.toString().isNullOrEmpty()) {
            verificationCodeText.value += number
        } else {
            verificationCodeText.value = verificationCodeText.value.replaceRange(index, index + 1, number)
        }
        if (number.isNotEmpty() && index < 3) {
            _focusedIndex.update { index + 1 }
        } else if (number.isEmpty() && index == 3) {
            isErrorShown.update { false }
        }
    }

    private fun getPreviousFocusedIndex(): Int? {
        return _focusedIndex.value?.minus(1)?.coerceAtLeast(0)
    }
}

sealed interface OtpAction {
    data class OnEnterNumber(val number: String, val index: Int) : OtpAction
    object OnKeyboardBack : OtpAction
}
