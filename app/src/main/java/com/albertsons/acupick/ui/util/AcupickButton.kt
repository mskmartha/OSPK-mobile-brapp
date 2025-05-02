package com.albertsons.acupick.ui.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold16

@Composable
fun AcupickButton(text: String, isEnabled: Boolean = false, modifier: Modifier, onClick: () -> Unit) {
    Button(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick() },
        shape = RoundedCornerShape(100.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = if (isEnabled) R.color.semiLightBlue else R.color.border)
        )
    ) {
        Text(
            text = text,
            color = colorResource(R.color.white),
            style = nunitoSemiBold16,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        )
    }
}
// we have to folow one color for the text,Button text color should be white irrespective of enabled or disabled state
// @Composable
// fun CustomButton(text: String, isEnabled: Boolean = false, modifier: Modifier, onClick: () -> Unit) {
//     Button(
//         modifier = modifier.fillMaxWidth(),
//         onClick = { onClick() },
//         shape = RoundedCornerShape(100.dp),
//         colors = ButtonDefaults.buttonColors(
//             backgroundColor = colorResource(id = if (isEnabled) R.color.semiLightBlue else R.color.coffeeLight)
//         )
//     ) {
//         Text(
//             text = text,
//             color = if (isEnabled) colorResource(R.color.white) else colorResource(R.color.grey_550),
//             style = nunitoSanReg16,
//             modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
//         )
//     }
// }
