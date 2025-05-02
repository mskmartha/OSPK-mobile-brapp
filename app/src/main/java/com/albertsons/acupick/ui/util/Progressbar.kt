package com.albertsons.acupick.ui.util
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.albertsons.acupick.R

@Composable
fun ProgressBar(isVisible: Boolean) {
    if (isVisible) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(alignment = Alignment.Center), color = colorResource(id = R.color.darkBlue))
        }
    }
}
