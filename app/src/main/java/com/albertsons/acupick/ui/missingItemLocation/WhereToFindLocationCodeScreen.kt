package com.albertsons.acupick.ui.missingItemLocation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.BottomSheetHandelBar
import com.albertsons.acupick.ui.util.Style

@Composable
fun WhereToFindLocationCodeScreen(viewModel: WhereToFindLocationViewModel) {
    MaterialTheme {
        Surface(modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), contentColor = Color.White, elevation = 8.dp) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = R.color.white))
                    .fillMaxHeight()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetHandelBar()
                Image(
                    painter = painterResource(id = R.drawable.ic_back_arrow),
                    contentScale = ContentScale.Inside,
                    contentDescription = "back button",
                    modifier = Modifier
                        .width(43.dp)
                        .height(43.dp).align(Alignment.Start)
                        .padding
                        (start = 5.dp, top = 8.dp)
                        .clickable(onClick = {
                            viewModel.onBackPressHandle()
                        })
                )
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.find_the_location_code), style = Style.poppinsMed20, textAlign = TextAlign.Center,
                    color =
                    colorResource(id = R.color.darkBrown)
                )
                Image(
                    modifier = Modifier
                        .width(232.dp)
                        .height(198.dp)
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    painter = painterResource(R.drawable.location_code), contentDescription = null
                )
                Text(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.the_9_digit_location_code_apperars), textAlign = TextAlign.Center,
                    style =
                    Style.nunitoSanReg16,
                    lineHeight = 24.sp,
                    color =
                    colorResource(id = R.color.grey_700)
                )
            }
        }
    }
}
