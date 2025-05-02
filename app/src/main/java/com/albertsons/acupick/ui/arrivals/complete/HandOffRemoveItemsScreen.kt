package com.albertsons.acupick.ui.arrivals.complete

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.AcupickButton
import com.albertsons.acupick.ui.util.BottomSheetHandelBar
import com.albertsons.acupick.ui.util.Style.nunitoSemiBold14
import com.albertsons.acupick.ui.util.Style.poppinsMedium20

@Composable
fun HandOffRemoveItemsScreen(
    viewModel: HandOffRemoveItemsViewModel,
) {
    val items by viewModel.items.collectAsState(emptyList())
    val isRxOrder by viewModel.isRxOrder.collectAsState(false)
    val areAllItemsChecked by remember(items) { derivedStateOf { viewModel.areAllItemsChecked() } }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        contentColor = Color.White,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            viewModel.onRemoveItemsCancelled()
                        }
                    }
            ) {
                BottomSheetHandelBar(79.dp)
                Text(
                    text = stringResource(R.string.remove_restricted_items_copy),
                    style = poppinsMedium20,
                    color = colorResource(id = R.color.grey_700),
                    modifier = Modifier
                        .padding(top = 44.dp)
                        .align(Alignment.CenterHorizontally)

                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(items) { item ->
                    ItemRemoveView(isRxOrder, item.description, item.imageUrl, qty = item.qty, isChecked = item.isChecked) { isChecked ->
                        viewModel.onRestrictedItemCheckedChange(item, isChecked)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AcupickButton(
                onClick = viewModel::onRemoveItemsConfirmed,
                text = stringResource(id = R.string.items_removed),
                isEnabled = areAllItemsChecked,
                modifier = Modifier.padding(top = 8.dp, start = 48.dp, end = 48.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
fun ItemRemoveView(
    isRxOrder: Boolean,
    description: String,
    imageUrl: String?,
    qty: Int? = null,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isRxOrder) 56.dp else 72.dp)
            .focusable(true)
    ) {
        if (isRxOrder) {
            RemoveRxItem(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                text = description
            )
        } else {
            RemoveRestrictedItem(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                text = description,
                qty = qty,
                imageUrl = imageUrl
            )
        }
    }
}

@Composable
fun RemoveRestrictedItem(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    text: String,
    qty: Int? = null,
    imageUrl: String?,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (checked) ImageVector.vectorResource(id = R.drawable.ic_checkbox_checked_state) else
                ImageVector.vectorResource(id = R.drawable.ic_checkbox_unchecked_state),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
                .clickable { onCheckedChange(!checked) },
            tint = Color.Unspecified
        )

        // Code to render an image next to Icon
        ConstraintLayout(modifier = Modifier.size(72.dp)/*.padding(start = 16.dp, end = 24.dp)*/) {
            val (image, textOverlay) = createRefs()

            AsyncImage(
                model = if (LocalInspectionMode.current) R.drawable.alert else imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    },
                placeholder = painterResource(id = R.drawable.ic_list_view_empty_image)
            )

            Box(
                modifier = Modifier
                    .constrainAs(textOverlay) {
                        bottom.linkTo(image.bottom)
                        start.linkTo(image.start)
                    }
                    .size(24.dp, 24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_rounded_corner_lighter_green),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = qty?.toString() ?: "",
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.kaleGreen),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Text(
            text = text,
            modifier = Modifier.align(Alignment.CenterVertically),
            style = nunitoSemiBold14,
            color = colorResource(id = R.color.grey_700),
        )
    }
}

@Composable
fun RemoveRxItem(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    text: String
) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = if (checked) ImageVector.vectorResource(id = R.drawable.ic_checkbox_checked_state) else
                ImageVector.vectorResource(id = R.drawable.ic_checkbox_unchecked_state),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
                .clickable { onCheckedChange(!checked) },
            tint = Color.Unspecified
        )

        Text(
            text = stringResource(id = R.string.rx_removal_formatting, text),
            modifier = Modifier.align(Alignment.CenterVertically),
            style = nunitoSemiBold14,
            color = colorResource(id = R.color.grey_700),
        )
    }
}

@Preview
@Composable
fun HandOffRemoveItemsScreenPreview() {
    val viewModel = HandOffRemoveItemsViewModel(Application())
    RemoveRestrictedItem(
        checked = false,
        onCheckedChange = {},
        text = "Item 1",
        qty = 1,
        imageUrl = "https://images.albertsons-media.com/is/image/ABS/289054700"
    )
}
