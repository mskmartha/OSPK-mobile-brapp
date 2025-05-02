package com.albertsons.acupick.ui.arrivals.destage.reportmissingbag

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.Style.nunitoSanReg16

@Composable
fun ReportMissingBagOrToteBottomSheetScreen(
    viewModel: ReportMissingBagSheetViewModel,
    isMfcSite: Boolean,
    isCurrentOrderHasLooseItem: Boolean,
    isCustomerBagPreference: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
            .background(color = colorResource(id = R.color.white))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize()
                .padding(start = 16.dp, end = 16.dp)
        ) {
            // show missing loose item label only if the current order has loose item and the site is not MFC site and customer not preferring bag
            if (!isCustomerBagPreference && isCurrentOrderHasLooseItem && !isMfcSite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clickable { viewModel.onMissingLooseItemLabelClicked() }
                ) {
                    Icon(
                        modifier = Modifier
                            .height(24.dp)
                            .width(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.no_label_icon),
                        contentDescription = stringResource(R.string.report_missing_tote_and_loose_item),
                        tint = colorResource(id = R.color.cattBlue)
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        style = nunitoSanReg16, color = colorResource(id = R.color.cattBlue),
                        text = stringResource(R.string.report_missing_tote_and_loose_item),
                    )
                }
            }
            // show missing loose item only if the current order has loose item and the site is not MFC site and customer not preferring bag
            if (!isCustomerBagPreference && isCurrentOrderHasLooseItem && !isMfcSite) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clickable { viewModel.onMissingLooseItemClicked() }
                ) {
                    Icon(
                        modifier = Modifier
                            .height(24.dp)
                            .width(24.dp),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_missing_loose),
                        contentDescription = stringResource(R.string.report_missing_loose_item),
                        tint = colorResource(id = R.color.cattBlue)
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        style = nunitoSanReg16, color = colorResource(id = R.color.cattBlue),
                        text = stringResource(R.string.report_missing_loose_item),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clickable { viewModel.onMissingBagLabelClicked() }
            ) {
                Icon(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.no_label_icon),
                    contentDescription = stringResource(R.string.bag_bypass_missing_bag_label_title),
                    tint = colorResource(id = R.color.cattBlue)
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    style = nunitoSanReg16, color = colorResource(id = R.color.cattBlue),
                    text = if (isMfcSite || !isCustomerBagPreference) stringResource(R.string.bag_bypass_missing_tote_label_title) else stringResource(R.string.bag_bypass_missing_bag_label_title),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .clickable { viewModel.onMissingBagClicked() },
            ) {
                Icon(
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                    imageVector = if (isMfcSite || !isCustomerBagPreference) ImageVector.vectorResource(R.drawable.missing_tote) else ImageVector.vectorResource(R.drawable.ic_missing_bag),
                    contentDescription = stringResource(R.string.bag_bypass_missing_bag_title),
                    tint = colorResource(id = R.color.cattBlue)
                )

                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = if (isMfcSite || !isCustomerBagPreference) stringResource(R.string.bag_bypass_missing_tote_title) else stringResource(R.string.bag_bypass_missing_bag_title),
                    style = nunitoSanReg16, color = colorResource(id = R.color.cattBlue),
                )
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color = colorResource(id = R.color.coffeeLight))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .clickable { viewModel.cancelClicked() }

            ) {
                Icon(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                    contentDescription = "Cancel",
                    tint = colorResource(id = R.color.cattBlue)
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = "Cancel",
                    style = nunitoSanReg16, color = colorResource(id = R.color.cattBlue)
                )
            }
        }
    }
}
