package com.albertsons.acupick.ui.arrivals.complete

import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.bottomsheetdialog.OtpAction
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.BottomSheetHandelBar
import com.albertsons.acupick.ui.util.Style.nunitoSanReg16
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold16
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold20

private const val FOCUS_REQUESTER_COUNT = 4

@Composable
fun AuthCodeVerificationScreen(
    verificationCode: String,
    isErrorShown: Boolean,
    customerName: String?,
    focusRequesters: List<FocusRequester>,
    onAction: (OtpAction) -> Unit,
    onConfirmClicked: () -> Unit,
    onCodeUnavailableClicked: () -> Unit
) {

    MaterialTheme {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentColor = Color.White,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSheetHandelBar(79.dp)

                Text(
                    text = stringResource(R.string.enter_auth_code, customerName ?: ""),
                    style = nunitoSemiBold20,
                    color = colorResource(id = R.color.grey_700),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp, start = 34.dp, end = 35.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(FOCUS_REQUESTER_COUNT) { index ->
                        VerificationCodeBox(
                            digit = verificationCode.getOrNull(index)?.toString() ?: "",
                            onDigitChanged = { newDigit ->
                                onAction(OtpAction.OnEnterNumber(newDigit, index))
                            },
                            focusRequester = focusRequesters[index],
                            isError = isErrorShown,
                            onKeyboardBack = {
                                onAction(OtpAction.OnKeyboardBack)
                            }
                        )
                    }
                }
                if (isErrorShown) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        modifier = Modifier
                            .padding(start = 65.dp)
                            .align(Alignment.Start),
                        text = stringResource(R.string.invalid_code),
                        style = nunitoSanReg16,
                        color = colorResource(R.color.chat_red)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                AcupickButton(
                    text = stringResource(R.string.confirm),
                    isEnabled = verificationCode.length == 4,
                    modifier = Modifier.padding(top = 8.dp, start = 48.dp, end = 48.dp, bottom = 12.dp),
                    onClick = onConfirmClicked
                )

                Text(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 30.dp)
                        .clickable { onCodeUnavailableClicked() }
                        .align(alignment = Alignment.CenterHorizontally),
                    text = stringResource(R.string.code_unavailable),
                    style = nunitoSemiBold16,
                    color = colorResource(R.color.semiLightBlue)
                )
            }
            LaunchedEffect(key1 = true) {
                focusRequesters[0].requestFocus()
            }
        }
    }
}

@Composable
fun VerificationCodeBox(
    digit: String,
    onDigitChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    onKeyboardBack: () -> Unit,
    isError: Boolean = false,
) {
    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = Color.Transparent
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        TextField(
            value = digit,
            onValueChange = { newDigit ->
                // Limit input to a single digit
                if (newDigit.length <= 1 && newDigit.isDigitsOnly()) {
                    onDigitChanged(newDigit)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .size(53.dp, 64.dp)
                .padding(4.dp)
                .border(
                    1.dp,
                    color = when {
                        isError -> colorResource(R.color.chat_red)
                        else -> colorResource(R.color.semiLightGray)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL && digit.isEmpty()) {
                        onKeyboardBack()
                    }
                    false
                },
            shape = RoundedCornerShape(8.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily(Font(R.font.nunito_sans_semibold)),
                fontSize = 20.sp,
                color = when {
                    isError -> colorResource(R.color.chat_red)
                    else -> colorResource(R.color.grey_700)
                },
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, // hide the indicator
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Unspecified,
            ),
        )
    }
}

@Preview
@Composable
fun AuthCodeVerificationScreenPreview() {
    AuthCodeVerificationScreen(
        verificationCode = "1234",
        isErrorShown = false,
        customerName = "John Doe",
        focusRequesters = remember { List(4) { FocusRequester() } },
        onAction = {},
        onConfirmClicked = {},
        onCodeUnavailableClicked = {}
    )
}
