package com.albertsons.acupick.ui.home

import com.albertsons.acupick.R
import com.albertsons.acupick.ui.util.DrawableIdHelper

sealed class HomeLoadingState(val drawableId: DrawableIdHelper? = null, val textResourceId: Int, val counter: Int = 0) {
    object Initial : HomeLoadingState(DrawableIdHelper.Id(R.drawable.ic_loading_state_home_screen), R.string.we_are_loading_the_order, 0)
    object Intermediate : HomeLoadingState(DrawableIdHelper.Id(R.drawable.ic_still_loading_home_screen), R.string.just_a_few_more_seconds, 1)
    object End : HomeLoadingState(DrawableIdHelper.Id(R.drawable.ic_empty_state_home_screen), R.string.there_are_no_orders_available_at_this_time, 0)
}
