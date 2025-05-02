package com.albertsons.acupick.ui.picklistitems

import androidx.annotation.StringRes
import com.albertsons.acupick.R

sealed class PickListItemsBottomPrompt(
    @StringRes val prompt: Int? = null,
    val onClickPrompt: (() -> Unit)? = null,
    val onClickKeyboard: (() -> Unit)? = null,
) {

    class Default(prompt: Int = R.string.please_scan_the_item, onClickKeyboard: (() -> Unit)? = null) : PickListItemsBottomPrompt(
        prompt = prompt,
        onClickKeyboard = onClickKeyboard
    )

    class Weight(prompt: Int = R.string.scan_weighed_item, onClickKeyboard: (() -> Unit)? = null) : PickListItemsBottomPrompt(
        prompt = prompt,
        onClickKeyboard = onClickKeyboard
    )

    class Eaches(prompt: Int, onClickPrompt: (() -> Unit)? = null) : PickListItemsBottomPrompt(
        prompt = prompt,
        onClickPrompt = onClickPrompt
    )

    object None : PickListItemsBottomPrompt()
}
