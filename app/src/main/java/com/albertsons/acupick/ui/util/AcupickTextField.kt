package com.albertsons.acupick.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.util.Style.nunitoSanReg12

@Composable
fun AcupickTextField(text: String, modifier: Modifier, maxCharacters: Int = 3, isError: Boolean = false, onValue: (String) -> Unit) {
    val isFocused = remember { mutableStateOf(false) }

    TextField(
        value = text,
        onValueChange = {
            if (it.length <= maxCharacters && it.isDigitsOnly()) {
                onValue(it)
            }
        },
        modifier = modifier
            .border(
                width = 1.dp, shape = RoundedCornerShape(8.dp),
                color = when {
                    isError -> colorResource(R.color.chat_red)
                    isFocused.value || text.isNotEmpty() -> colorResource(R.color.text_field_focused)
                    else -> colorResource(R.color.grey_550)
                }
            )
            .onFocusChanged
            { focusState -> isFocused.value = focusState.isFocused },
        placeholder = {
            Text(
                text = "000",
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.nunito_sans_semibold)), fontSize = 16.sp,
                    color = colorResource(
                        R.color.semiLightGray
                    ),
                    letterSpacing = 1.sp
                )
            )
        },
        textStyle = TextStyle(fontFamily = FontFamily(Font(R.font.nunito_sans_semibold)), fontSize = 16.sp, color = colorResource(R.color.chat_coffee), letterSpacing = 1.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, // hide the indicator
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colorResource(id = R.color.chat_coffee)
        )
    )
}

@Composable
fun CommentBox(modifier: Modifier, maxCharacters: Int = 50, label: String = "", isError: Boolean = false, onValue: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val isFocused = remember { mutableStateOf(false) }
    Box {
        TextField(
            value = text,
            onValueChange = {
                if (it.length <= maxCharacters) {
                    text = it
                    onValue(it)
                }
            },
            maxLines = 2,
            label = {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(bottom = 8.dp)
                        /*style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.nunito_sans_regular)), color = colorResource(
                                R.color.grey_700
                            )
                        )*/
                    )
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp, shape = RoundedCornerShape(8.dp),
                    color = when {
                        isError -> colorResource(R.color.chat_red)
                        isFocused.value || text.isNotEmpty() -> colorResource(R.color.text_field_focused)
                        else -> colorResource(R.color.grey_550)
                    }
                )
                .onFocusChanged
                { focusState -> isFocused.value = focusState.isFocused },
            textStyle = TextStyle(fontFamily = FontFamily(Font(R.font.nunito_sans_regular)), fontSize = 16.sp, color = colorResource(R.color.grey_700)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, // hide the indicator
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = colorResource(id = R.color.chat_coffee),
                cursorColor = colorResource(id = R.color.chat_coffee)
            )
        )
        Text(
            text = "${text.length}/$maxCharacters",
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .align(Alignment.BottomEnd)
                .offset(y = 20.dp),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.nunito_sans_regular)), fontSize = 12.sp, letterSpacing = 0.3.sp,
                color = colorResource(
                    R.color.chat_coffee
                )
            )
        )
    }
}

@Composable
fun CustomTextField(
    modifier: Modifier,
    maxCharacters: Int = 50,
    label: String = "",
    isError: String? = null,
    onValue: (String) -> Unit,
    onDone: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    val isFocused = remember { mutableStateOf(false) }
    Column {
        TextField(
            value = text,
            onValueChange = {
                if (it.length <= maxCharacters) {
                    text = it
                    onValue(it)
                }
            },
            maxLines = 1,
            singleLine = true,
            label = {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.nunito_sans_regular)),
                            color = colorResource(R.color.grey_550)
                        )
                    )
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isError.isNotNullOrEmpty() -> colorResource(R.color.semiDarkRed)
                        isFocused.value || text.isNotEmpty() -> colorResource(R.color.semiLightBlue)
                        else -> colorResource(R.color.semiLightGray)
                    }
                )
                .onFocusChanged { focusState -> isFocused.value = focusState.isFocused },
            textStyle = TextStyle(
                fontFamily = FontFamily(Font(R.font.nunito_sans_regular)),
                fontSize = 16.sp,
                color = colorResource(R.color.grey_700)
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, // hide the indicator
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = colorResource(id = R.color.chat_coffee),
                cursorColor = colorResource(id = R.color.chat_coffee)
            )
        )
        if (isError.isNotNullOrEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_red_warning),
                    contentDescription = stringResource(id = R.string.error)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = isError.toString(),
                    style = nunitoSanReg12,
                    color = colorResource(R.color.semiDarkRed)
                )
            }
        }
    }
}
