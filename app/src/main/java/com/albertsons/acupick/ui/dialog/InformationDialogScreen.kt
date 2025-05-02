package com.albertsons.acupick.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.isNotNullOrEmpty
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.Style.nunitoSanReg16
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold16
import com.albertsons.acupick.ui.util.Style.poppinsMed20
import com.albertsons.acupick.ui.util.annotateBoldWord

@Composable
fun InformationDialogScreen(data: CustomDialogViewData, viewModel: CustomDialogViewModel) {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp)),
            contentColor = Color.White, elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.white))
                    .padding(horizontal = 24.dp, vertical = 40.dp)
                    .wrapContentHeight()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = data.title, style = poppinsMed20, color = colorResource(id = R.color.darkBrown), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                data.largeImage?.let {
                    Image(
                        painter = painterResource(id = it), contentDescription = stringResource(id = R.string.print_gift_note),
                        Modifier
                            .width(128.dp)
                            .height(128.dp)
                            .padding(top = 16.dp)
                    )
                }
                data.body?.let {
                    if (data.boldWord.isNotNullOrEmpty()) {
                        Text(
                            text = data.body.annotateBoldWord(data.boldWord),
                            style = nunitoSanReg16,
                            color = colorResource(id = R.color.darkBrown),
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(text = it, style = nunitoSanReg16, color = colorResource(id = R.color.darkBrown), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                    }
                }
                data.bodyWithBold?.let {
                    Text(text = it, style = nunitoSanReg16, color = colorResource(id = R.color.darkBrown), modifier = Modifier.padding(top = 8.dp), lineHeight = 24.sp, textAlign = TextAlign.Center)
                }
                data.secondaryBody?.let {
                    if (data.questionBody.isNotNullOrEmpty()) {
                        Text(
                            text = data.secondaryBody.annotateBoldWord(data.questionBody),
                            style = nunitoSanReg16,
                            color = colorResource(id = R.color.darkBrown),
                            modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center
                        )
                    } else {
                        Text(text = it, style = nunitoSanReg16, color = colorResource(id = R.color.darkBrown), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
                    }
                }
                data.positiveButtonText?.let {
                    AcupickButton(text = it, isEnabled = true, modifier = Modifier.padding(top = 24.dp), onClick = { viewModel.onPositiveButtonClick() })
                }
                data.negativeButtonText?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable { viewModel.onNegativeButtonClick() },
                        color = colorResource(id = R.color.cattBlue), style = nunitoSemiBold16, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
