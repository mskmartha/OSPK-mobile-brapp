package com.albertsons.acupick.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.albertsons.acupick.R

@Composable
fun BottomSheetHandelBar(width: Dp = 56.dp) {
    Column(Modifier.fillMaxWidth()) {
        Image(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(width)
                .height(4.dp).align(alignment = Alignment.CenterHorizontally),
            painter = painterResource(id = R.drawable.ic_handle),
            contentDescription = "Bottomsheet Handle Bar",
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )
    }
}
