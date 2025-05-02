package com.albertsons.acupick.ui.picklists

import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.albertsons.acupick.R
import com.albertsons.acupick.infrastructure.utils.AM_PM_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.HOUR_MINUTE_TIME_FORMATTER
import com.albertsons.acupick.infrastructure.utils.formattedWith
import java.time.ZoneId
import java.time.ZonedDateTime

@BindingAdapter("app:setFormattedDate")
fun TextView.setFormattedDate(zonedDateTime: ZonedDateTime?) {
    text = zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(HOUR_MINUTE_TIME_FORMATTER)
}

@BindingAdapter("app:setFormattedDateAmPm")
fun TextView.setFormattedDateAmPm(zonedDateTime: ZonedDateTime?) {
    text = zonedDateTime?.withZoneSameInstant(ZoneId.systemDefault()).formattedWith(AM_PM_TIME_FORMATTER)
}

@BindingAdapter("app:dueDayLabel")
fun AppCompatTextView.setPrepickDueVisible(dueDay: Long = 0L) {
    text = context.getString(
        when {
            dueDay == 1L -> R.string.due
            dueDay > 1L -> R.string.due_in
            else -> R.string.stage_by
        }
    )
}
