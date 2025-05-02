package com.albertsons.acupick.ui.missingItemLocation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.models.MissingItemLocationParams
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.AcupickTextField
import com.albertsons.acupick.ui.util.BottomSheetHandelBar
import com.albertsons.acupick.ui.util.CommentBox
import com.albertsons.acupick.ui.util.ProgressBar
import com.albertsons.acupick.ui.util.Style.nunitoSanReg10
import com.albertsons.acupick.ui.util.Style.nunitoSanReg12
import com.albertsons.acupick.ui.util.Style.nunitoSanReg16
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold12
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold14
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold16
import com.albertsons.acupick.ui.util.Style.poppinsMed20

@Composable
fun MissingItemLocationScreen(viewModel: MissingItemLocationViewModel, missingItemLocationParams: MissingItemLocationParams) {
    val isValidInput = viewModel.isValidInput.collectAsState(false)
    val isErrorShown = viewModel.isErrorShown.collectAsState(false)
    val isLoading = viewModel.isLoading.collectAsState(false)
    val aisle = viewModel.aisle.collectAsState("")
    val section = viewModel.section.collectAsState("")
    val shelf = viewModel.shelf.collectAsState("")

    MaterialTheme {
        val scrollState = rememberScrollState()
        Surface(modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), contentColor = Color.White, elevation = 8.dp) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(start = 31.dp, end = 31.dp)
                    .verticalScroll(scrollState),
            ) {
                BottomSheetHandelBar()
                Text(
                    text = stringResource(R.string.where_you_find),
                    style = poppinsMed20,
                    color = colorResource(id = R.color.darkBrown),
                    modifier = Modifier
                        .padding(top = 41.dp)
                        .align(alignment = Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(R.string.others_locate_the_item),
                    style = nunitoSanReg16,
                    fontWeight = FontWeight(400),
                    color = colorResource(R.color.grey_700),
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
                ItemDetails(missingItemLocationParams.itemImage, missingItemLocationParams.itemDescription, missingItemLocationParams.itemUpcId)
                Text(
                    modifier = Modifier
                        .padding(top = 23.dp)
                        .fillMaxWidth(),
                    text = stringResource(R.string.digit_location_code),
                    style = nunitoSemiBold12,
                    color = colorResource(R.color.grey_700)
                )
                OtpInputFeilds(
                    aisle.value,
                    section.value,
                    shelf.value,
                    viewModel::onAisleChanged,
                    viewModel::onSectionChanged,
                    viewModel::onShelfChanged,
                    isErrorShown.value
                )
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            viewModel.onClickWhereToFindLocationCode()
                        },
                    text = stringResource(R.string.where_to_find_location_code),
                    style = nunitoSanReg10,
                    color = colorResource(R.color.cattBlue)
                )
                CommentBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                        .padding(top = 23.dp),
                    label = stringResource(R.string.describe_the_location_optional),
                    onValue = viewModel::onCommentChanged
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    text = stringResource(R.string.e_g_near_left_entrance),
                    style = nunitoSanReg12,
                    color = colorResource(R.color.chat_coffee)
                )
                AcupickButton(
                    text = stringResource(R.string.add_location),
                    isEnabled = isValidInput.value,
                    modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    onClick = viewModel::onClickAddLocation
                )
                Text(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 30.dp)
                        .clickable { viewModel.onClickNotNow() }
                        .align(alignment = Alignment.CenterHorizontally),
                    text = stringResource(R.string.not_now),
                    style = nunitoSemiBold16,
                    color = colorResource(R.color.semiLightBlue)
                )
            }
            ProgressBar(isLoading.value)
        }
    }
}

@Composable
fun ItemDetails(image: String? = null, itemName: String, upcNo: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 21.dp, top = 15.dp, end = 21.dp)
    ) {
        AsyncImage(
            model = if (image.isNullOrEmpty()) R.drawable.ic_item_details_empty_image else image,
            placeholder = painterResource(R.drawable.ic_item_details_empty_image),
            contentDescription = "item",
            modifier = Modifier
                .size(width = 56.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.FillBounds
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = itemName,
                style = nunitoSemiBold14,
                color = colorResource(R.color.grey_700),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = upcNo,
                style = nunitoSanReg12,
                color = colorResource(R.color.grey_700)
            )
        }
    }
}

@Composable
fun OtpInputFeilds(
    aisle: String,
    section: String,
    shelf: String,
    onAisleChanged: (String) -> Unit,
    onSectionChanged: (String) -> Unit,
    onShelfChanged: (String) -> Unit,
    isError: Boolean,
) {
    val aisleFocusRequester = remember { FocusRequester() }
    val sectionFocusRequester = remember { FocusRequester() }
    val shelfFocusRequester = remember { FocusRequester() }

    Column {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {

            AcupickTextField(
                text = aisle,
                modifier = Modifier
                    .height(56.dp)
                    .width(75.dp)
                    .focusRequester(aisleFocusRequester),
                onValue = {
                    onAisleChanged(it)
                },
                isError = isError
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(color = colorResource(id = R.color.black))
            )
            AcupickTextField(
                text = section,
                modifier = Modifier
                    .height(56.dp)
                    .width(75.dp)
                    .focusRequester(sectionFocusRequester),
                onValue = {
                    onSectionChanged(it)
                },
                isError = isError
            )
            Spacer(
                modifier = Modifier
                    .width(10.dp)
                    .height(1.dp)
                    .background(color = colorResource(id = R.color.black))
            )
            AcupickTextField(
                text = shelf,
                modifier = Modifier
                    .height(56.dp)
                    .width(75.dp)
                    .focusRequester(shelfFocusRequester),
                onValue = {
                    onShelfChanged(it)
                },
                isError = isError
            )
        }
        LaunchedEffect(Unit) {
            aisleFocusRequester.requestFocus()
        }

        LaunchedEffect(key1 = aisle) {
            if (aisle.length == 3) {
                sectionFocusRequester.requestFocus()
            }
        }

        LaunchedEffect(key1 = section) {
            if (section.length == 3) {
                shelfFocusRequester.requestFocus()
            }
        }
        if (isError) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Image(painter = painterResource(R.drawable.ic_loc_error), contentDescription = "error")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.enter_valid_location),
                    style = nunitoSanReg16,
                    color = colorResource(R.color.error)
                )
            }
        }
    }
}
