package com.albertsons.acupick

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.duginterjection.failureReasonTextValue
import com.albertsons.acupick.data.duginterjection.isAppear
import com.albertsons.acupick.data.duginterjection.isDestagingRxOrder
import com.albertsons.acupick.data.duginterjection.isMaxHandOffAssigned
import com.albertsons.acupick.data.model.ActivityType
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.FulfillmentType
import com.albertsons.acupick.data.model.OrderType
import com.albertsons.acupick.data.model.notification.NOTIFICATION_DATA
import com.albertsons.acupick.data.model.notification.NotificationData
import com.albertsons.acupick.data.model.notification.NotificationType
import com.albertsons.acupick.data.model.notification.getNotificationReceivedCount
import com.albertsons.acupick.data.model.notification.isChatNotification
import com.albertsons.acupick.data.model.notification.isChatPickerNotification
import com.albertsons.acupick.data.model.notification.isCustomerArrivalTimeRequired
import com.albertsons.acupick.data.model.notification.isCustomerArrivalTimeRequiredForAllUser
import com.albertsons.acupick.data.model.notification.isFinalNotification
import com.albertsons.acupick.data.model.notification.toNotificationData
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.model.request.RecordNotificationRequestDto
import com.albertsons.acupick.data.model.response.ActivityDto
import com.albertsons.acupick.data.model.response.ArrivedInterjectionNotificationDto
import com.albertsons.acupick.data.repository.ConversationsRepository
import com.albertsons.acupick.data.repository.PickRepository
import com.albertsons.acupick.data.repository.PushNotificationsRepository
import com.albertsons.acupick.data.repository.SiteRepository
import com.albertsons.acupick.data.repository.UserRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.ellipse
import com.albertsons.acupick.ui.MainActivity
import com.albertsons.acupick.ui.util.isChatScreen
import com.albertsons.acupick.ui.util.isCompleteHandOffFlow
import com.albertsons.acupick.ui.util.isDestagingFlow
import com.albertsons.acupick.ui.util.isStagingFlow
import com.albertsons.acupick.ui.util.orFalse
import com.albertsons.acupick.ui.util.shouldNotShowchatNotification
import com.albertsons.acupick.ui.util.shouldNotShowchatPickerJoinedNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

class AcuPickMessagingService() : FirebaseMessagingService(), KoinComponent {
    private var apps: Application? = null

    constructor(application: Application) : this() {
        apps = application
    }

    private val pushNotificationsRepository: PushNotificationsRepository by inject()
    private val userRepo: UserRepository by inject()
    private val pickRepo: PickRepository by inject()
    private val siteRepo: SiteRepository by inject()
    private val conversationsRepository: ConversationsRepository by inject()
    val acuPickLogger: AcuPickLoggerInterface by inject()

    // /////////////////////////////////////////////////////////////////////////
    // Service Callbacks
    // /////////////////////////////////////////////////////////////////////////
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.e("FCM - From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Timber.e("FCM - Message data payload: ${remoteMessage.data}")
            createNotification(remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        Timber.e("FCM - Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    // /////////////////////////////////////////////////////////////////////////
    // Helpers
    // /////////////////////////////////////////////////////////////////////////
    private fun sendRegistrationToServer(token: String?) {
        Timber.e("FCM - sendRegistrationTokenToServer($token)")
        token?.let {
            pushNotificationsRepository.saveFcmToken(it)
            if (userRepo.isLoggedIn.value && userRepo.user.value?.selectedStoreId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(NonCancellable) {
                        pushNotificationsRepository.sendFcmTokenToBackend()
                    }
                }
            }
        }
    }

    fun createNotification(messageData: Map<String, String>) {
        val data = messageData.toNotificationData()
        val pendingIntent = getPendingIntent(data, false)

        getNotificationChannelId(data)?.also { channelId ->

            // Check if the channel exists, if not create it
            val notificationManager = (apps ?: this).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.getNotificationChannel(channelId).apply {
                if (this == null) {
                    acuPickLogger.e("No channel found for $channelId, creating new channel")
                    CoroutineScope(Dispatchers.IO).launch {
                        conversationsRepository.sendLog(
                            ApiResult.Failure.GeneralFailure(channelId, networkCallName = NetworkCalls.NOTIFICATION_ERROR.value),
                            false
                        )
                    }
                    notificationManager.deleteNotificationChannel(channelId)
                    // Create the specific notificiation channel if it is not available
                    createNotificationChannels((apps ?: this@AcuPickMessagingService.application), false, channelId)
                } else if (sound == null) {
                    acuPickLogger.e("Null sound value, channel id - $channelId sound - $sound")
                    CoroutineScope(Dispatchers.IO).launch {
                        conversationsRepository.sendLog(
                            ApiResult.Failure.GeneralFailure(channelId, networkCallName = NetworkCalls.NOTIFICATION_SOUND_ERROR.value),
                            false
                        )
                    }
                }
            }
            // TODO: DUG2.0 Once customerArrivalTime available in notification payload will uncomment the below line of code.
            // recordNotificationReceived(data)
            // TODO: DUG2.0 Once customerArrivalTime available in notification payload will remove the below block of code.
            /**
             * We are checking for [NotificationType] if the notificationType is not [NotificationType.ARRIVED_INTERJECTION]
             * We are just calling the recordNotification API will not wait for the API response.
             * if the notificationType is [NotificationType.ARRIVED_INTERJECTION] and [NotificationData.customerArrivedTime] !=null
             * also we check whether the notification type is chat message or chat picker joined/left notification
             * Also in this case calling the recordNotification API will not wait for the API response.
             */
            if (data.isCustomerArrivalTimeRequired().not() &&
                data.isCustomerArrivalTimeRequiredForAllUser().not() &&
                data.isChatNotification().not() &&
                data.isChatPickerNotification().not()
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    recordNotificationReceived(data)
                }
            }

            fun createChatPickerNotification(data: NotificationData, channelId: String): NotificationCompat.Builder {
                val notificationLayout = RemoteViews((apps ?: this).packageName, R.layout.notification_picker_joined).apply {
                    setTextViewText(R.id.notification_title, getTitle(data))
                    setTextViewText(R.id.notification_text, getBodyForPicker(data))
                }

                return NotificationCompat.Builder(apps ?: this, channelId)
                    .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationLayout)
                    .setShowWhen(true) // Add this line to show the time as 'now'
                    .setAutoCancel(true)
            }

            fun createDefaultNotification(data: NotificationData, channelId: String): NotificationCompat.Builder {
                return NotificationCompat.Builder(apps ?: this, channelId)
                    .setContentTitle(getTitle(data))
                    .setContentText(getBody(data))
                    .setLargeIcon(BitmapFactory.decodeResource((apps ?: this).resources, getIcon(data)))
                    .setAutoCancel(true)
            }

            val notificationBuilder = when (data.notificationType) {
                NotificationType.CHAT_PICKER -> createChatPickerNotification(data, channelId)
                else -> createDefaultNotification(data, channelId)
            }

            when (data.notificationType) {
                NotificationType.PICKING -> {
                    addArrivedCTA(pendingIntent, data, notificationBuilder)
                    checkForOnGoingOrHybridNotification(data, pendingIntent, notificationBuilder)
                }

                NotificationType.ARRIVED, NotificationType.ARRIVING -> {
                    if (data.serviceLevel == OrderType.FLASH || data.serviceLevel == OrderType.FLASH3P) {
                        if (userRepo.isLoggedIn.value) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val activityDto = getOrderActivityDetails(data)
                                if (shouldShowFlashNotification(activityDto?.assignedTo?.userId, activityDto?.actType)) {
                                    handleFlashDriverNotification(data, pendingIntent, notificationBuilder, activityDto?.actType)
                                }
                            }
                        }
                    } else {
                        addArrivedCTA(pendingIntent, data, notificationBuilder)
                        checkForOnGoingOrHybridNotification(data, pendingIntent, notificationBuilder)
                    }
                }

                NotificationType.ARRIVED_INTERJECTION, NotificationType.ARRIVED_INTERJECTION_ALL_USER -> {
                    // TODO: DUG2.0 Will umcomment below method once customerArrivalTime available in notification payload
                    // handleDugInterjectionNotification(data, pendingIntent)

                    // TODO: DUG2.0 Once customerArrivalTime available in notification payload will remove the below block of code.
                    /**
                     * In this block of code if the notificationType is [NotificationType.ARRIVED_INTERJECTION] and [NotificationData.customerArrivedTime]
                     * available in notification payload we will not make an API call will use it in interjection dialog.
                     * If the [NotificationData.customerArrivedTime]  not available in notification payload we have to make an API call and wait for the response.
                     * On success we will use [ArrivedInterjectionNotificationDto.customerArrivedTime] from the API response and update the notification data to show the wait time.
                     */
                    data.customerArrivedTime?.let {
                        handleDugInterjectionNotification(data, pendingIntent)
                    } ?: run {
                        CoroutineScope(Dispatchers.IO).launch {
                            recordNotificationReceived(data).also { result ->
                                data.copy(customerArrivedTime = result?.customerArrivedTime).also { data ->
                                    handleDugInterjectionNotification(data, pendingIntent)
                                }
                            }
                        }
                    }
                }

                NotificationType.DISMISS -> {
                    // DISMISS notifications are silent, and used for dismissing other types
                    dismissNotification(data, notificationBuilder)
                    if (shouldHandleDismissDugInterjection(data.customerOrderNumber)) {
                        handleDismissDugInterjection(data, pendingIntent)
                    }
                }
                NotificationType.CHAT -> {
                    if (shouldShowChatNotification(data = data)) {
                        sendChatNotification(data, notificationBuilder, getPendingIntentForChat(data, false))
                    }
                }
                NotificationType.CHAT_PICKER -> {
                    if (shouldShowChatPickerJoinedNotication()) {
                        sendChatPickerJoinedNotification(data, notificationBuilder, getPendingIntentForChat(data, false))
                    }
                }
                else -> {}
            }
        }
    }

    private fun handleDismissDugInterjection(data: NotificationData, pendingIntent: PendingIntent) {
        sendHybridNotification(pendingIntent, data)
    }

    /**
     * Validate DUG Interjection is showing on the screen and
     * also validate [orderNumber] of [NotificationType.DISMISS] for the appeared DUG Interjection.
     */
    private fun shouldHandleDismissDugInterjection(orderNumber: String?): Boolean = pushNotificationsRepository.getDugInterjectionState().isAppear() &&
        (pushNotificationsRepository.getDugInterjectionState() as DugInterjectionState.Appear).orderNumber == orderNumber

    private fun handleDugInterjectionNotification(data: NotificationData, pendingIntent: PendingIntent) {
        if (shouldShowDugInterjection()) {
            dismissNotification(data, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_DISMISS))
            sendHybridNotification(pendingIntent, data)
        }
    }

    private fun dismissNotification(data: NotificationData, builder: NotificationCompat.Builder) {
        // DISMISS notifications are silent, and used for dismissing other types
        builder.apply {
            priority = NotificationCompat.PRIORITY_LOW
            setTimeoutAfter(10)
            sendNotification(data, builder)
        }
    }

    private fun sendNotification(data: NotificationData, notificationBuilder: NotificationCompat.Builder) {
        try {
            notificationBuilder.setSmallIcon(R.drawable.ic_acu_pick_logo_wht)
            val notificationManager = (apps ?: this).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val id = data.customerOrderNumber?.toIntOrNull() ?: 0
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            val channelId = getNotificationChannelId(data)
            acuPickLogger.e("No channel found for $channelId, creating new channel")
            CoroutineScope(Dispatchers.IO).launch {
                conversationsRepository.sendLog(
                    ApiResult.Failure.GeneralFailure(channelId ?: data.notificationType.toString(), networkCallName = NetworkCalls.NOTIFICATION_ERROR.value),
                    false
                )
            }
        }
    }

    private fun sendChatNotification(data: NotificationData, notificationBuilder: NotificationCompat.Builder, intent: PendingIntent) {
        try {
            notificationBuilder.setSmallIcon(R.drawable.ic_acu_pick_logo_wht)
            notificationBuilder.setAutoCancel(true)
            notificationBuilder.setContentIntent(intent)
            val notificationManager = (apps ?: this).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val id = System.currentTimeMillis().toInt()
            pushNotificationsRepository.saveChatPushId(id)
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            acuPickLogger.e("No channel found for ChatNotification, creating new channel")
            CoroutineScope(Dispatchers.IO).launch {
                conversationsRepository.sendLog(
                    ApiResult.Failure.GeneralFailure("ChatNotification", networkCallName = NetworkCalls.NOTIFICATION_ERROR.value),
                    false
                )
            }
        }
    }

    private fun sendChatPickerJoinedNotification(data: NotificationData, notificationBuilder: NotificationCompat.Builder, intent: PendingIntent) {
        try {
            notificationBuilder.setSmallIcon(R.drawable.ic_acu_pick_logo_wht)
            notificationBuilder.setAutoCancel(true)
            notificationBuilder.setContentIntent(intent)
            val notificationManager = (apps ?: this).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val id = System.currentTimeMillis().toInt()
            pushNotificationsRepository.saveChatPushId(id)
            notificationManager.notify(id, notificationBuilder.build())
        } catch (e: Exception) {
            acuPickLogger.e("No channel found for ChatPickerJoined creating new channel")
            CoroutineScope(Dispatchers.IO).launch {
                conversationsRepository.sendLog(
                    ApiResult.Failure.GeneralFailure("ChatPickerJoined", networkCallName = NetworkCalls.NOTIFICATION_ERROR.value),
                    false
                )
            }
        }
    }

    /** Since we're using the [PendingIntent] to show the (fullscreen) notification rather than the for
     *  handling the notification click, we need to cancel the old [PendingIntent] and create a new one
     *  with the isShowingNotification flag set */
    private fun sendHybridNotification(pendingIntent: PendingIntent, data: NotificationData) {
        pendingIntent.cancel()
        getPendingIntent(data, true).send()
    }

    private fun getPendingIntent(notificationData: NotificationData, isShowingNotification: Boolean): PendingIntent {
        val data = notificationData.copy(isShowingHybridMessage = isShowingNotification)

        val intent = Intent((apps ?: this), MainActivity::class.java).apply {
            putExtras(bundleOf(Pair(NOTIFICATION_DATA, data)))
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        // the requestCode must be unique for the retrieved PendingIntent to be unique
        val requestCode = data.actId?.toInt() ?: 0
        return PendingIntent.getActivity((apps ?: this), requestCode, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getPendingIntentForChat(notificationData: NotificationData, isShowingNotification: Boolean): PendingIntent {
        val data = notificationData.copy(isShowingHybridMessage = isShowingNotification)

        val intent = Intent((apps ?: this), MainActivity::class.java).apply {
            putExtras(bundleOf(Pair(NOTIFICATION_DATA, data)))
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        // the requestCode must be unique for the retrieved PendingIntent to be unique
        val requestCode = System.currentTimeMillis().toInt()
        return PendingIntent.getActivity((apps ?: this), requestCode, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun shouldMergeArrivedEventDataOnCounterOneAndTwo(data: NotificationData) = siteRepo.isDugInterjectionEnabled && data.fulfillmentType == FulfillmentType.DUG

    private fun getTitle(data: NotificationData): CharSequence {
        val customerName = "${data.customerFirstName} ${data.customerLastName}"
        return when (data.notificationType) {
            NotificationType.ARRIVED ->
                when {
                    shouldMergeArrivedEventDataOnCounterOneAndTwo(data) -> getString(
                        when (data.notificationCounter) {
                            1, 2 -> R.string.notification_title_arrived_1
                            else -> R.string.notification_title_arrived_3
                        },
                        customerName
                    )

                    else -> getString(
                        when (data.notificationCounter) {
                            1 -> R.string.notification_title_arrived_1
                            2 -> R.string.notification_title_arrived_2
                            else -> R.string.notification_title_arrived_3
                        },
                        customerName
                    )
                }

            NotificationType.ARRIVING -> getString(R.string.notification_title_arriving, customerName)
            NotificationType.PICKING -> getString(
                when (data.notificationCounter) {
                    1 -> R.string.notification_title_picking_1
                    2 -> R.string.notification_title_picking_2
                    else -> R.string.notification_title_picking_3
                }
            ).format(
                if (data.serviceLevel == OrderType.FLASH3P) {
                    getString(R.string.notification_title_partnerpick)
                } else {
                    getString(R.string.notification_title_flash)
                }
            )

            NotificationType.CHAT -> {
                setColorOnText(R.string.message_Notification_title, data.customerName, false)
            }
            NotificationType.CHAT_PICKER -> {
                if (data.pickerJoined.orFalse()) {
                    setColorOnText(R.string.chat_picker_joined_title, data.pickerName, true)
                } else {
                    setColorOnText(R.string.chat_picker_left_title, data.pickerName, true)
                }
            }

            else -> ""
        }
    }

    private fun setColorOnText(@StringRes id: Int, name: String?, withBold: Boolean): Spanned {
        val formatted = "<font color=\"${ContextCompat.getColor((apps ?: this), R.color.semiLightBlue)}\">${apps?.getString(id, name) ?: ""}</font>"
        return if (withBold) {
            HtmlCompat.fromHtml(formatted, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            HtmlCompat.fromHtml("<b>$formatted</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun getBodyForPicker(data: NotificationData): SpannableString {
        return if (data.pickerJoined.orFalse()) {
            val message = (apps ?: this).getString(R.string.chat_picker_joined_body, "${data.pickerName}")
            val pickerName = data.pickerName ?: ""
            val startIdx = message.indexOf(pickerName)
            val endIdx = startIdx + pickerName.length
            SpannableString(message).apply {
                if (pickerName.isNotEmpty()) {
                    setSpan(StyleSpan(Typeface.BOLD), message.indexOf(pickerName), endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else {
            val message = (apps ?: this).getString(R.string.chat_picker_notification_left_body, "${data.pickerName}")
            SpannableString(message)
        }
    }

    private fun getBody(data: NotificationData): String {
        val stageByTime = data.stageByTime
        return when (data.notificationType) {
            NotificationType.ARRIVED ->
                when {
                    shouldMergeArrivedEventDataOnCounterOneAndTwo(data) -> getString(
                        when (data.notificationCounter) {
                            1, 2 -> R.string.notification_body_arrived_1
                            else -> R.string.notification_body_arrived_3
                        }
                    )

                    else -> getString(
                        when (data.notificationCounter) {
                            1 -> R.string.notification_body_arrived_1
                            2 -> R.string.notification_body_arrived_2
                            else -> R.string.notification_body_arrived_3
                        }
                    )
                }

            NotificationType.ARRIVING -> getString(R.string.notification_body_arriving)
            NotificationType.PICKING -> {
                stageByTime?.let {
                    getString(
                        when (data.notificationCounter) {
                            1 -> R.string.notification_body_picking_1
                            2 -> R.string.notification_body_picking_2
                            else -> R.string.notification_body_picking_3
                        },
                        stageByTime
                    )
                } ?: run { "" }
            }

            NotificationType.CHAT -> "\"${data.customerMessage.ellipse(40)}\""
            else -> ""
        }
    }

    private fun getIcon(data: NotificationData): Int {
        return when (data.fulfillmentType) {
            FulfillmentType.DUG -> {
                when (data.notificationType) {
                    NotificationType.ARRIVED -> when {
                        shouldMergeArrivedEventDataOnCounterOneAndTwo(data) -> when (data.notificationCounter) {
                            1, 2 -> R.drawable.ic_dug_blue
                            else -> R.drawable.ic_dug_red
                        }

                        else -> when (data.notificationCounter) {
                            1 -> R.drawable.ic_dug_blue
                            2 -> R.drawable.ic_dug_orange
                            else -> R.drawable.ic_dug_red
                        }
                    }

                    NotificationType.CHAT -> {
                        Timber.d("chat trasparent")
                        R.drawable.transparent_push_icon
                    }

                    else -> R.drawable.ic_dug_blue
                }
            }

            else -> {
                when (data.notificationType) {
                    NotificationType.ARRIVED -> when (data.notificationCounter) {
                        1 -> R.drawable.ic_3pl_blue
                        2 -> R.drawable.ic_3pl_orange
                        else -> R.drawable.ic_3pl_red
                    }

                    NotificationType.PICKING -> when (data.notificationCounter) {
                        1 -> R.drawable.ic_flash_blue
                        2 -> R.drawable.ic_flash_orange
                        else -> R.drawable.ic_flash_red
                    }

                    NotificationType.CHAT -> {
                        Timber.d("chat trasparent")
                        R.drawable.transparent_push_icon
                    }

                    else -> R.drawable.ic_flash_blue
                }
            }
        }
    }

    private fun getCtaColor(data: NotificationData): Int {
        return ContextCompat.getColor(
            (apps ?: this),
            when (data.notificationType) {
                NotificationType.ARRIVED -> when {
                    shouldMergeArrivedEventDataOnCounterOneAndTwo(data) -> when (data.notificationCounter) {
                        1, 2 -> R.color.darkBlue
                        else -> R.color.error
                    }

                    else -> when (data.notificationCounter) {
                        1 -> R.color.darkBlue
                        2 -> R.color.darkestOrange
                        else -> R.color.error
                    }
                }

                NotificationType.PICKING -> when (data.notificationCounter) {
                    1 -> R.color.darkBlue
                    2 -> R.color.darkestOrange
                    else -> R.color.error
                }

                else -> R.color.darkBlue
            }

        )
    }

    private fun getCtaString(data: NotificationData): String {
        return getString(
            when (data.notificationType) {
                NotificationType.PICKING -> R.string.notification_cta_begin_order
                else -> R.string.notification_cta_begin_handoff
            }
        )
    }

    private fun addArrivedCTA(pendingIntent: PendingIntent, data: NotificationData, notificationBuilder: NotificationCompat.Builder) {
        // Show CTA for ARRIVING and ARRIVED notifications
        val actionButtonText = HtmlCompat.fromHtml(
            "<font color=\"${getCtaColor(data)}\">${getCtaString(data)}</font>", HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        notificationBuilder.apply {
            addAction(0, actionButtonText, pendingIntent)
            setContentIntent(pendingIntent)
            setTimeoutAfter(TIMEOUT_MS)
        }
    }

    private fun isFlashInterjectionHybrid(data: NotificationData) =
        siteRepo.isFlashInterjectionEnabled &&
            pushNotificationsRepository.getDugInterjectionState().isAppear().not() && // Validating if DUG interjection is currently showing then FLASH interjection wont be shown
            data.serviceLevel == OrderType.FLASH &&
            data.notificationType == NotificationType.PICKING &&
            data.isFinalNotification() &&
            data.isShowingHybridMessage

    private fun isUrgentArrivalHybrid(data: NotificationData) =
        if (siteRepo.isHybridPickingDialogEnabled) {
            (data.notificationType == NotificationType.ARRIVED || data.notificationType == NotificationType.ARRIVING) &&
                data.isFinalNotification() && data.isShowingHybridMessage
        } else false

    private fun isOnGoingNotification(data: NotificationData) = data.isFinalNotification()

    private fun checkForOnGoingOrHybridNotification(data: NotificationData, pendingIntent: PendingIntent, builder: NotificationCompat.Builder) {
        when {
            isFlashInterjectionHybrid(data) || isUrgentArrivalHybrid(data) -> {
                dismissNotification(data, NotificationCompat.Builder((apps ?: this), NOTIFICATION_CHANNEL_ID_DISMISS))
                sendHybridNotification(pendingIntent, data)
            }

            isOnGoingNotification(data) -> {
                builder.setOngoing(true)
                sendNotification(data, builder)
            }

            else -> sendNotification(data, builder)
        }
    }

    // TODO: DUG INTERJECTION keeping channel id same for dug interjection. After testing will finalize it or will create separate channel id for DUG interjection
    private fun getNotificationChannelId(data: NotificationData): String? {
        return when (data.notificationType) {
            NotificationType.ARRIVED, NotificationType.ARRIVING, NotificationType.ARRIVED_INTERJECTION, NotificationType.ARRIVED_INTERJECTION_ALL_USER -> {
                if (shouldShowHeadsUpNotification(data)) NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_HIGH else NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_DEFAULT
            }

            NotificationType.PICKING -> {
                if (shouldShowHeadsUpNotification(data)) NOTIFICATION_CHANNEL_ID_NEW_PICKING_HIGH else NOTIFICATION_CHANNEL_ID_NEW_PICKING_DEFAULT
            }

            NotificationType.DISMISS -> NOTIFICATION_CHANNEL_ID_DISMISS
            NotificationType.CHAT -> NOTIFICATION_CHANNEL_ID_NEW_CHAT_MESSAGE_DEFAULT
            NotificationType.CHAT_PICKER -> NOTIFICATION_CHANNEL_ID_NEW_CHAT_PICKER_DEFAULT
            else -> null
        }
    }

    private fun shouldShowHeadsUpNotification(data: NotificationData): Boolean {
        return when (pushNotificationsRepository.getCurrentDestination()) {
            R.id.handOffFragment, R.id.handOffInterstitialFragment,
            R.id.manualEntryHandOffFragment, R.id.updateCustomerChangeStatusFragment,
            R.id.updateCustomerAddFragment,
            -> false

            R.id.destageOrderFragment -> data.notificationType != NotificationType.PICKING
            else -> true
        }
    }
    private fun shouldShowChatNotification(data: NotificationData): Boolean {
        val destinationId = pushNotificationsRepository.getCurrentDestination()
        return destinationId.shouldNotShowchatNotification().not() ||
            (destinationId.isChatScreen() && pushNotificationsRepository.getInProgressChatOrderId().orEmpty() != data.customerOrderNumber)
    }

    private fun shouldShowChatPickerJoinedNotication(): Boolean {
        Timber.d("shouldShowChatNotication")
        return pushNotificationsRepository.getCurrentDestination().shouldNotShowchatPickerJoinedNotification().not()
    }

    /**
     * To validate DUG interjection conditions if all these conditions meet it will be appear on the screen.
     */
    private fun shouldShowDugInterjection(): Boolean = siteRepo.isDugInterjectionEnabled &&
        pushNotificationsRepository.getCurrentDestination().isCompleteHandOffFlow().not() &&
        pushNotificationsRepository.getCurrentDestination().isDestagingFlow().not() &&
        pushNotificationsRepository.getDugInterjectionState().isAppear().not() &&
        pushNotificationsRepository.getDugInterjectionState().isMaxHandOffAssigned().not() &&
        pushNotificationsRepository.getDugInterjectionState().isDestagingRxOrder().not()

    private suspend fun getOrderActivityDetails(data: NotificationData): ActivityDto? {
        if (data.actId == null) {
            Timber.e("Activity Id is null. AcuPickMessagingService(getOrderActivityDetails), Order Id-${data.customerOrderNumber}")
        }
        return when (val result = pickRepo.getActivityDetails(data.actId.toString(), false)) {
            is ApiResult.Success -> {
                result.data
            }

            else -> null
        }
    }

    private fun shouldShowFlashNotification(userId: String?, status: ActivityType?) = userId == userRepo.user.value?.userId || status == ActivityType.PICKUP ||
        status == ActivityType.THREEPL_PICKUP

    private fun handleFlashDriverNotification(data: NotificationData, pendingIntent: PendingIntent, builder: NotificationCompat.Builder, activityType: ActivityType?) {
        val destination = pushNotificationsRepository.getCurrentDestination()

        when (activityType) {
            ActivityType.PICK_PACK, ActivityType.DROP_OFF -> {
                if ((application as AcuPickApplication).isAppOnForeground() && destination.isStagingFlow()) {
                    sendHybridNotification(pendingIntent, data)
                } else {
                    sendNotification(data, builder)
                }
            }

            else -> {
                addArrivedCTA(pendingIntent, data, builder)
                checkForOnGoingOrHybridNotification(data, pendingIntent, builder)
            }
        }
    }

    // TODO: DUG2.0 To get customerArrivedTime from recordNotification API
    @SuppressLint("HardwareIds")
    private suspend fun recordNotificationReceived(data: NotificationData): ArrivedInterjectionNotificationDto? {
        return if (userRepo.isLoggedIn.value) {
            when (
                val result = pushNotificationsRepository.recordNotificationReceived(
                    RecordNotificationRequestDto(
                        actId = data.actId,
                        deviceId = Settings.Secure.getString((apps ?: this@AcuPickMessagingService).contentResolver, Settings.Secure.ANDROID_ID),
                        notificationCounter = data.getNotificationReceivedCount(),
                        notificationType = data.notificationType,
                        receivedTimeStamp = ZonedDateTime.now(),
                        failureReasonCode = when (data.notificationType) {
                            NotificationType.ARRIVED_INTERJECTION -> pushNotificationsRepository.getDugInterjectionState().failureReasonTextValue()
                            NotificationType.ARRIVED_INTERJECTION_ALL_USER -> pushNotificationsRepository.getDugInterjectionState().failureReasonTextValue()
                            else -> null
                        }
                    )
                )
            ) {
                is ApiResult.Success -> result.data
                else -> null
            }
        } else null
    }

    companion object {
        // notifications should timeout or self-dismiss after 1 hour
        private const val TIMEOUT_MS = 60 * 60 * 1000L

        // old Channel id's that are deleted.
        private const val NOTIFICATION_CHANNEL_ID_ARRIVAL_HIGH = "handoff_channel_high"
        private const val NOTIFICATION_CHANNEL_ID_ARRIVAL_DEFAULT = "handoff_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_PICKING_HIGH = "picking_channel_high"
        private const val NOTIFICATION_CHANNEL_ID_PICKING_DEFAULT = "picking_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_CHAT_DEFAULT = "chat_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_NEW_CHAT_DEFAULT = "new_chat_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_CHAT_PICKER_DEFAULT = "chat_picker_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_DISMISS = "dismiss_channel"

        // new channel id's
        private const val NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_HIGH = "new_handoff_channel_high"
        private const val NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_DEFAULT = "new_handoff_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_NEW_PICKING_HIGH = "new_picking_channel_high"
        private const val NOTIFICATION_CHANNEL_ID_NEW_PICKING_DEFAULT = "new_picking_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_NEW_CHAT_PICKER_DEFAULT = "new_chat_picker_channel_default"
        private const val NOTIFICATION_CHANNEL_ID_NEW_CHAT_MESSAGE_DEFAULT = "new_chat_message_channel_default"

        fun createNotificationChannels(app: Application, createAllChannels: Boolean = false, channelId: String? = null) {
            val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // custom notification sounds
            // val resourceUriPrefix = "android.resource://${app.applicationContext.packageName}/"

            // We are moving away from resource id's to direct path for the sound files
            val arrivalSoundUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    app.packageName + "/raw/notification_arrival"
            )

            val pickingSoundUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    app.packageName + "/raw/notification_picking"
            )

            val chatSoundUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    app.packageName + "/raw/notification_chat"
            )

            // val arrivalSoundUri = Uri.parse(resourceUriPrefix + R.raw.notification_arrival)
            // val pickingSoundUri = Uri.parse(resourceUriPrefix + R.raw.notification_picking)
            // val chatSoundUri = Uri.parse(resourceUriPrefix + R.raw.notification_chat)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // deleting all the existing channels and creating new channels with new channel id for changing the sound resource for each of the channel.
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_CHAT_DEFAULT)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_ARRIVAL_HIGH)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_ARRIVAL_DEFAULT)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_PICKING_HIGH)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_PICKING_DEFAULT)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_NEW_CHAT_DEFAULT)
            notificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID_CHAT_PICKER_DEFAULT)

            fun createChannel(id: String, nameResId: Int, importance: Int, soundUri: Uri? = null, enableVibration: Boolean = true) {
                val channel = NotificationChannel(id, app.getString(nameResId), importance).apply {
                    soundUri?.let { setSound(it, audioAttributes) }
                    enableVibration(enableVibration)
                }
                notificationManager.createNotificationChannel(channel)
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_DEFAULT) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_DEFAULT,
                    R.string.notification_channel_name_arrival_default,
                    NotificationManager.IMPORTANCE_DEFAULT,
                    arrivalSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_HIGH) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_ARRIVAL_HIGH,
                    R.string.notification_channel_name_arrival_high,
                    NotificationManager.IMPORTANCE_HIGH,
                    arrivalSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_PICKING_DEFAULT) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_PICKING_DEFAULT,
                    R.string.notification_channel_name_arrival_default,
                    NotificationManager.IMPORTANCE_DEFAULT,
                    pickingSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_PICKING_HIGH) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_PICKING_HIGH,
                    R.string.notification_channel_name_picking_high,
                    NotificationManager.IMPORTANCE_HIGH,
                    pickingSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_CHAT_MESSAGE_DEFAULT) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_CHAT_MESSAGE_DEFAULT,
                    R.string.notification_channel_name_chat_default,
                    NotificationManager.IMPORTANCE_HIGH,
                    chatSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_NEW_CHAT_PICKER_DEFAULT) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_NEW_CHAT_PICKER_DEFAULT,
                    R.string.notification_channel_name_chat_picker_default,
                    NotificationManager.IMPORTANCE_HIGH,
                    chatSoundUri
                )
            }

            if (createAllChannels || channelId == NOTIFICATION_CHANNEL_ID_DISMISS) {
                createChannel(
                    NOTIFICATION_CHANNEL_ID_DISMISS,
                    R.string.notification_channel_name_dismiss,
                    NotificationManager.IMPORTANCE_MIN, null, false
                )
            }
        }
    }
}
