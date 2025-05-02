package com.albertsons.acupick.data.repository

import android.content.SharedPreferences
import com.albertsons.acupick.data.duginterjection.DugInterjectionState
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.request.CreateUpdateDeviceInfoRequestDto
import com.albertsons.acupick.data.model.request.RecordNotificationRequestDto
import com.albertsons.acupick.data.model.response.ArrivedInterjectionNotificationDto
import com.albertsons.acupick.data.network.logging.LoggingDataProvider

interface PushNotificationsRepository : Repository {
    fun getFcmToken(): String?
    fun saveFcmToken(token: String)
    fun saveChatPushId(id: Int)
    fun getChatPushIds(): List<String>
    fun clearChatIds()
    fun setCurrentDestination(destinationId: Int)
    fun getCurrentDestination(): Int
    fun setDugInterjectionState(value: DugInterjectionState)
    fun getDugInterjectionState(): DugInterjectionState
    fun getInProgressChatOrderId(): String?
    fun setInProgressChatOrderId(orderNumber: String?)
    suspend fun recordNotificationReceived(notificationDto: RecordNotificationRequestDto): ApiResult<ArrivedInterjectionNotificationDto>
    suspend fun sendFcmTokenToBackend()
}

class PushNotificationsRepositoryImplementation(
    private val apsRepository: ApsRepository,
    private val sharedPrefs: SharedPreferences,
    private val loggingDataProvider: LoggingDataProvider,
) : PushNotificationsRepository {

    companion object {
        private const val FCM_TOKEN = "fcmToken"
        private const val CHAT_IDS = "chatId"
        private const val CURRENT_DESTINATION = "currentDestination"
    }

    private var dugInterjectionState: DugInterjectionState = DugInterjectionState.None

    private var inProgressChatOrderId: String? = null
    override fun getFcmToken(): String? {
        return sharedPrefs.getString(FCM_TOKEN, null)
    }

    override fun saveFcmToken(token: String) {
        sharedPrefs.edit().putString(FCM_TOKEN, token).apply()
    }

    override fun saveChatPushId(id: Int) {
        val set: MutableSet<String> = HashSet<String>().apply {
            addAll(getChatPushIds())
            add(id.toString())
        }
        sharedPrefs.edit().putStringSet(CHAT_IDS, set).commit()
    }

    override fun getChatPushIds(): List<String> {
        return sharedPrefs.getStringSet(CHAT_IDS, null)?.toList() ?: emptyList()
    }

    override fun clearChatIds() {
        sharedPrefs.edit().remove(CHAT_IDS).commit()
    }
    override fun setCurrentDestination(destinationId: Int) {
        sharedPrefs.edit().putInt(CURRENT_DESTINATION, destinationId).apply()
    }

    override fun getCurrentDestination(): Int {
        return sharedPrefs.getInt(CURRENT_DESTINATION, 0)
    }

    override fun setDugInterjectionState(value: DugInterjectionState) {
        dugInterjectionState = value
    }

    override fun getDugInterjectionState(): DugInterjectionState {
        return dugInterjectionState
    }

    override fun getInProgressChatOrderId(): String? {
        return inProgressChatOrderId
    }

    override fun setInProgressChatOrderId(orderNumber: String?) {
        inProgressChatOrderId = orderNumber
    }

    override suspend fun recordNotificationReceived(notificationDto: RecordNotificationRequestDto): ApiResult<ArrivedInterjectionNotificationDto> {
        return apsRepository.recordNotificationReceived(notificationDto)
    }

    override suspend fun sendFcmTokenToBackend() {
        getFcmToken()?.let { token ->
            apsRepository.createUpdateDeviceInfo(
                CreateUpdateDeviceInfoRequestDto(
                    deviceId = loggingDataProvider.deviceId,
                    deviceToken = token,
                    siteId = loggingDataProvider.storeId,
                )
            )
        }
    }
}
