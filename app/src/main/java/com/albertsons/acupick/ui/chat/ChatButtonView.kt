package com.albertsons.acupick.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.albertsons.acupick.R
import com.albertsons.acupick.data.model.CustomerChatInfo
import com.albertsons.acupick.data.model.asFirstNameLastInitialDotString
import com.albertsons.acupick.ui.MainActivityViewModel

const val MAX_ITEM_TO_BE_SHOWN = 2
const val OFFSET_BY = 20
const val DIALOG_BOTTOM_MARGIN_WITH_BOTTOM_NAV_BAR = 160
const val DIALOG_BOTTOM_MARGIN_WITHOUT_BOTTOM_NAV_BAR = 92

@Composable
fun ChatIconWithTooltip(onChatClicked: (orderNumber: String) -> Unit, activityViewModel: MainActivityViewModel? = null) {
    val viewModel: ChatButtonViewModel = viewModel()
    val isTooltipVisible by viewModel.isTooltipVisible.collectAsState()
    val showUnreadMessages by viewModel.showUnreadMessages.collectAsState()
    val notificationCount by viewModel.notificationCount.collectAsState()
    val showNotificationCount by viewModel.showNotificationCount.collectAsState()
    val showChatButton by viewModel.showChatButton.collectAsState()

    activityViewModel?.let {
        val shouldNavigate by it.blockStagingHandleEvent.collectAsState(initial = false)
        if (shouldNavigate && !isTooltipVisible) {
            viewModel.onFloatingActionButtonClick(onChatClicked)
            it.blockStagingHandleEvent.value = false
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (isTooltipVisible) {
                        viewModel.setTooltipVisibility(false)
                    }
                })
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        if (isTooltipVisible) {
            TooltipDialog(viewModel, onChatClicked)
        }

        if (showChatButton) {
            FloatingActionButtonWithNotification(
                showNotificationCount = showNotificationCount,
                showUnreadMessages = showUnreadMessages,
                notificationCount = notificationCount,
                onClick = { viewModel.onFloatingActionButtonClick(onChatClicked) }
            )
        }
    }
}

@Composable
fun FloatingActionButtonWithNotification(
    showNotificationCount: Boolean,
    showUnreadMessages: Boolean,
    notificationCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.BottomEnd),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
            backgroundColor = colorResource(R.color.semiLightBlue)
        ) {
            Image(
                painter = painterResource(id = R.drawable.chat_bubble),
                contentDescription = stringResource(id = R.string.image_chat_buble_content_description),
                contentScale = ContentScale.Fit,
            )
        }
        if (showUnreadMessages) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.Red, CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                if (showNotificationCount) {
                    Text(
                        text = notificationCount.toString(),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.nunito_sans_bold)),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TooltipDialog(viewModel: ChatButtonViewModel, onChatClicked: (orderNumber: String) -> Unit) {
    val customerChatInfoList by viewModel.customerChatInfoList.collectAsState(emptyList())
    val isPickingScreen by viewModel.isPickingScreen.collectAsState()

    Dialog(
        onDismissRequest = { viewModel.setTooltipVisibility(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        if (viewModel.isTooltipVisible.value) {
                            viewModel.setTooltipVisibility(false)
                        }
                    })
                }
        ) {

            val dialogMarginBottom = isPickingScreen.takeIf { it }?.let { DIALOG_BOTTOM_MARGIN_WITHOUT_BOTTOM_NAV_BAR } ?: DIALOG_BOTTOM_MARGIN_WITH_BOTTOM_NAV_BAR
            Card(
                modifier = Modifier
                    .padding(end = dimensionResource(id = R.dimen.home_screen_timer_char_text), bottom = dialogMarginBottom.dp)
                    .width(dimensionResource(id = R.dimen.chat_menu_popup_width))
                    .wrapContentHeight()
                    .align(Alignment.BottomEnd)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.White,
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringResource(id = R.string.open_chats),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.nunito_sans_semibold)),
                            fontSize = 20.sp,
                            color = colorResource(id = R.color.grey_700),
                            lineHeight = 24.sp
                        )
                    )
                    Spacer(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth()
                            .background(color = Color.White)
                    )

                    LazyColumn {
                        items(customerChatInfoList.size) { index ->
                            OrderChatDetailRow(
                                customerChatInfo = customerChatInfoList[index],
                                onChatClicked = onChatClicked,
                                viewModel = viewModel
                            )
                            if (index < customerChatInfoList.size - 1) {
                                Spacer(
                                    modifier = Modifier
                                        .height(1.dp)
                                        .fillMaxWidth()
                                        .background(colorResource(id = R.color.coffeeLight))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderChatDetailRow(
    customerChatInfo: CustomerChatInfo,
    onChatClicked: (orderNumber: String) -> Unit,
    viewModel: ChatButtonViewModel
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .height(68.dp)
            .padding(start = 8.dp, end = 8.dp)
            .clickable {
                viewModel.setTooltipVisibility(false)
                onChatClicked(customerChatInfo.customerOrderNumber)
            }
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.wrapContentSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (customerChatInfo.hasUnreadMessages) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = Color.Red, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = customerChatInfo.asFirstNameLastInitialDotString(customerChatInfo.substitutionItemImages?.size ?: 0),
                        color = Color.Black,
                        maxLines = 1,
                        style = TextStyle(
                            fontFamily = if (customerChatInfo.hasUnreadMessages) FontFamily(Font(R.font.nunito_sans_bold)) else FontFamily(Font(R.font.nunito_sans_regular)),
                            fontSize = 16.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (customerChatInfo.hasUnreadMessages && customerChatInfo.isCustomerTyping) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = Color.White, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (customerChatInfo.isCustomerTyping) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.dots_animation))
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        customerChatInfo.substitutionItemImages?.let {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LazyRow {
                    items(it.size) { index ->
                        when (index) {
                            0, 1 -> CircularImageViewWithBorder(it[index], index, it.size)
                            2 -> {
                                Text(
                                    text = "+${it.size - MAX_ITEM_TO_BE_SHOWN}",
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.nunito_sans_regular)),
                                        fontSize = 16.sp,
                                        color = colorResource(R.color.grey_700)
                                    ),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularImageViewWithBorder(image: String, index: Int, imageCount: Int) {
    val offsetValue = when {
        imageCount == MAX_ITEM_TO_BE_SHOWN && index == 0 -> OFFSET_BY
        imageCount > MAX_ITEM_TO_BE_SHOWN && index == 0 -> OFFSET_BY
        else -> 0
    }
    AsyncImage(
        model = image.ifEmpty { R.drawable.ic_item_details_empty_image },
        placeholder = painterResource(R.drawable.ic_item_details_empty_image),
        contentDescription = stringResource(id = R.string.image_content_description),
        modifier = Modifier
            .offset(offsetValue.dp)
            .size(42.dp)
            .border(1.dp, colorResource(R.color.border), CircleShape)
            .clip(CircleShape),
        contentScale = ContentScale.Inside
    )
}

@Preview(showBackground = true)
@Composable
fun ChatIconWithTooltipPreview() {
    ChatIconWithTooltip(onChatClicked = { _ -> })
}
