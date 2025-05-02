package com.albertsons.acupick.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.Style.nunitoSanReg16
import com.albertsons.acupick.ui.util.Style.nunitoSansBold14
import com.albertsons.acupick.ui.util.Style.poppinsMed20

@Preview
@Composable
private fun OnePlGiftingDialogPreview() {
    OnePlGiftingDialogScreen(
        viewModel = null,
        title = "Collect 4 gift Notes",
        largeImage = R.drawable.ic_gift_note,
        body = "Attach gift notes to the matching orders before handoff",
        secondaryBody = "To re-print or view gift notes please check the web dashboard",
        boldWord = "1PL C4",
        positiveButtonText = "Print 4 Gift Notes",
    )
}

@Composable
fun OnePlGiftingDialogScreen(
    viewModel: CustomDialogViewModel? = null,
    title: String,
    largeImage: Int?,
    body: String?,
    secondaryBody: String?,
    boldWord: String?,
    positiveButtonText: String?,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            contentColor = Color.White, elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.white))
                    .padding(horizontal = 24.dp, vertical = 40.dp)
                    .wrapContentHeight()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = poppinsMed20,
                    color = colorResource(id = R.color.darkBrown),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                largeImage?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = stringResource(id = R.string.print_gift_note),
                        Modifier
                            .width(88.dp)
                            .height(88.dp)
                            .padding(top = 16.dp)
                    )
                }

                body?.let {
                    Text(
                        text = it,
                        style = nunitoSanReg16,
                        color = colorResource(id = R.color.darkBrown),
                        modifier = Modifier.padding(top = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    boldWord?.let {
                        Text(
                            text = it,
                            style = nunitoSansBold14,
                            color = colorResource(id = R.color.darkBrown),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.padding(start = 4.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_fullfillment_onepl),
                        contentDescription = stringResource(id = R.string.onePl)
                    )
                }

                positiveButtonText?.let {
                    AcupickButton(
                        text = it,
                        isEnabled = true,
                        modifier = Modifier.padding(top = 24.dp),
                        onClick = { viewModel?.onPositiveButtonClick() }
                    )
                }

                secondaryBody?.let {
                    Text(
                        text = it,
                        style = nunitoSanReg16,
                        color = colorResource(id = R.color.darkerGray),
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
