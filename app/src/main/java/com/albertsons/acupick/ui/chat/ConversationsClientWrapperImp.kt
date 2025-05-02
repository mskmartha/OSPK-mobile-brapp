package com.albertsons.acupick.ui.chat

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import com.albertsons.acupick.data.model.ApiResult
import com.albertsons.acupick.data.model.alsoOnSuccess
import com.albertsons.acupick.data.model.request.ChatErrorData
import com.albertsons.acupick.data.model.request.NetworkCalls
import com.albertsons.acupick.data.repository.ConversationsClientWrapper
import com.albertsons.acupick.data.repository.OsccRepository
import com.albertsons.acupick.domain.AcuPickLoggerInterface
import com.albertsons.acupick.infrastructure.utils.updateToken
import com.squareup.moshi.Moshi
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.extensions.createAndSyncConversationsClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import com.twilio.conversations.extensions.addListener

class ConversationsClientWrapperImp(
    private val applicationContext: Application,
    val osccRepository: OsccRepository,
    val acuPickLogger: AcuPickLoggerInterface,
    val moshi: Moshi
) : ConversationsClientWrapper {

    private var deferredClient = CompletableDeferred<ConversationsClient>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    override suspend fun getConversationsClient(): ConversationsClient = deferredClient.await() // Business logic will wait until conversationsClient created

    /**
     * Get token and call createClient if token is not null
     */
    override suspend fun create(userId: String): ApiResult<String> {
        acuPickLogger.e("create $userId}")
        var startTime = 0L
        when (val token = osccRepository.getTwilioToken(userId)) {
            is ApiResult.Success -> {
                return try {
                    Timber.d("token: ${token.data}")
                    startTime = System.currentTimeMillis()
                    deferredClient = CompletableDeferred()
                    val client = createAndSyncConversationsClient(applicationContext, token.data)
                    this.deferredClient.complete(client)
                    client.addListener(
                        onTokenAboutToExpire = { updateToken(userId, notifyOnFailure = false) },
                        onTokenExpired = { updateToken(userId, notifyOnFailure = true) },
                    )
                    token
                } catch (e: Exception) {
                    val timeElapsed = System.currentTimeMillis() - startTime
                    this.deferredClient.cancel()
                    val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val percent = WifiManager.calculateSignalLevel(wifiInfo.rssi, 100)
                    val errorData = ChatErrorData(errorMessage = "Time elapsed - $timeElapsed : WIFI rssi - ${wifiInfo.rssi}- strength -$percent : Error - ${e.localizedMessage.orEmpty()}")
                        .toJsonString(moshi)
                    ApiResult.Failure.GeneralFailure(errorData, networkCallName = NetworkCalls.TWILIO_SDK_INITIALIZATION_FAILURE.value)
                }
            }

            is ApiResult.Failure -> {
                this.deferredClient.cancel()
                Timber.d("$token")
                val chatErrorData = ChatErrorData(
                    errorMessage = token.toString()
                ).toJsonString(moshi)
                return ApiResult.Failure.GeneralFailure(chatErrorData, networkCallName = NetworkCalls.GET_TWILIO_TOKEN_FAILURE.value)
            }
        }
    }

    override suspend fun shutdown() {
        Timber.d("shutdown client")
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    private fun updateToken(userId: String, notifyOnFailure: Boolean) = coroutineScope.launch {
        Timber.d("updateToken notifyOnFailure: $notifyOnFailure")
        acuPickLogger.e("updateToken notifyOnFailure- $notifyOnFailure")
        runCatching {
            osccRepository.getTwilioToken(userId).alsoOnSuccess { twilioToken ->
                getConversationsClient().updateToken(twilioToken)
            }
        }.onFailure {
            acuPickLogger.e("${it.localizedMessage} notifyOnFailure- $notifyOnFailure")
        }
    }
}
