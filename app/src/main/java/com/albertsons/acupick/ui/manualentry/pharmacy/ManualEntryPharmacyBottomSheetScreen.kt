package com.albertsons.acupick.ui.manualentry.pharmacy

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.manualentry.ManualEntryPharmacyParams
import com.albertsons.acupick.ui.picklistitems.ScanTarget
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.BottomSheetHandelBar
import com.albertsons.acupick.ui.util.CustomTextField
import com.albertsons.acupick.ui.util.Style.nunitoSansBold20
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold16

@Composable
fun ManualEntryPharmacyScreen(viewModel: ManualEntryPharmacyViewModel, manualEntryParams: ManualEntryPharmacyParams) {

    val continueEnabled = viewModel.continueEnabled.observeAsState()
    val hint = viewModel.hint.observeAsState(initial = "")
    val error = viewModel.error.observeAsState(initial = null)
    val focusRequester = remember { FocusRequester() }
    MaterialTheme {
        Surface(modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), contentColor = Color.White, elevation = 8.dp) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(start = 31.dp, end = 31.dp)

            ) {
                BottomSheetHandelBar()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Text(
                        text = manualEntryParams.shortOrderId ?: "",
                        style = nunitoSansBold20,
                        color = colorResource(R.color.grey_600),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(15.dp)
                            .background(color = colorResource(id = R.color.grey_550))
                    )
                    Text(
                        text = "#${manualEntryParams.customerOrderNumber}",
                        style = nunitoSansBold20,
                        color = colorResource(R.color.grey_600),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = manualEntryParams.customerName ?: "",
                    style = nunitoSemiBold16,
                    color = colorResource(R.color.grey_700),
                    modifier = Modifier.padding(top = 6.dp)
                )
                CustomTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(top = 24.dp).focusRequester(focusRequester),
                    label = hint.value, isError = stringResource(id = error.value ?: R.string.empty),
                    onDone = { viewModel.onContinueButtonClicked() },
                    onValue = viewModel::onTexChanged,

                )

                AcupickButton(
                    text = stringResource(R.string.continue_cta),
                    isEnabled = continueEnabled.value ?: false,
                    modifier = Modifier.padding(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    onClick = { viewModel.onContinueButtonClicked() },
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable

fun PreviewManualEntryPharmacyScreen() {
    ManualEntryPharmacyScreen(
        viewModel = ManualEntryPharmacyViewModel(app = Application()),
        manualEntryParams = ManualEntryPharmacyParams(
            "467575", ScanTarget.PharmacyArrivalLabel,
            customerOrderNumber = "467575"
        )
    )
}
