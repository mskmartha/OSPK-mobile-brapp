package com.albertsons.acupick.ui.picklists.open

import androidx.annotation.StringRes
import com.albertsons.acupick.R

enum class OrderCategoryUi(@StringRes val resourceId: Int) {
    ALL(R.string.all_order),
    FLASH(R.string.flash_order),
    PARTNER_PICK(R.string.partnerpick_order),
    REGULAR(R.string.standard_order),
    EXPRESS(R.string.express_order)
}
