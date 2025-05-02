package com.albertsons.acupick

import com.albertsons.acupick.ui.util.EventAction
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class FirebaseAnalyticsImpl : FirebaseAnalyticsInterface {

    override fun logEvent(
        eventCategory: String,
        eventAction: EventAction,
        eventLabel: String,
        valuePairList: List<Pair<String, String>>?
    ) {
        Firebase.analytics.logEvent(eventAction.value) {
            param(EventKey.EVENT_CATEGORY, eventCategory)
            param(EventKey.EVENT_LABEL, eventLabel)
            valuePairList?.forEach {
                param(it.first, it.second)
            }
        }
    }

    override fun setUserPropertyValue(key: String, value: String) {
        Firebase.analytics.setUserProperty(key, value)
    }

    override fun setuserId(userId: String) {
        Firebase.analytics.setUserId(userId)
    }
}

object EventKey {
    const val EVENT_CATEGORY = "event_category"
    const val EVENT_LABEL = "event_label"
    const val ACTIVITY_ID = "activity_id"
    const val PICKLIST_ID = "picklist_id"
    const val ORDER_ID = "order_id"
    const val BOTTOM_SHEET_NAME = "bottom_sheet_name"
}

object EventCategory {
    const val PICKING = "picking"
    const val DE_STAGING = "de_staging"
    const val CHAT = "chats"
    const val BOTTOM_SHEET = "bottom_sheet"
    const val SUBSTITUTION = "substitution"
}

object EventLabel {
    const val PICKLIST_LISTVIEW = "picklist_listview"
    const val PICKLIST_CARDVIEW = "picklist_cardView"
    const val CHAT_RETRY_BTN = "chat_retry_btn"
    const val CHAT_SWAP_BTN = "chat_swap_btn"
    const val CHAT_SEND_BTN = "chat_send_btn"
    const val CHAT_IMAGE_UPLOAD_BTN = "chat_image_upload_btn"
    const val CHAT_IMAGE_PREVIEW_CLOSE = "chat_image_preview_close_btn"
    const val CHAT_IMAGE_CLICK = "chat_image_click"
    const val STORE_ID = "store_id"
    const val BOTTOM_SHEET_STATE_OPEN = "bottom_sheet_state_open"
    const val BOTTOM_SHEET_STATE_CLOSE = "bottom_sheet_state_close"
    const val SUBSTITUTION_CONFIRM_EXIT = "substitution_confirm_exit"
    const val SWAP_SUBSTITUTION_CONFIRM_EXIT = "swap_substitution_confirm_exit"
}
