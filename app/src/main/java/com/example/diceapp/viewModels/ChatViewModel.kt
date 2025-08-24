package com.example.diceapp.viewModels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diceapp.models.ChatMessage
import com.example.diceapp.models.GetMessagesResponse
import com.example.diceapp.models.MessageType
import com.example.diceapp.models.SendMessageRequest
import com.example.diceapp.util.parseAndRollDice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var currentCampaignId by mutableStateOf<String?>(null)
        private set

    private var autoRefreshJob: Job? = null
    private val lastRefreshTime = AtomicLong(0L)
    private val minRefreshInterval = 2000L

    fun setCampaign(campaignId: String) {
        Log.d("ChatViewModel", "setCampaign(campaignId=$campaignId)")
        if (currentCampaignId != campaignId) {
            currentCampaignId = campaignId
            _messages.value = emptyList()
            _errorMessage.value = null
            stopAutoRefresh()
        }
    }

    fun stopAutoRefresh() {
        Log.d("ChatViewModel", "stopAutoRefresh()")
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun loadMessages(
        context: Context,
        campaignId: String? = null,
        isAutoRefresh: Boolean = false
    ) {
        Log.d("ChatViewModel", "loadMessages(campaignId=$campaignId, isAutoRefresh=$isAutoRefresh)")
        val id = campaignId ?: currentCampaignId ?: return

        val currentTime = System.currentTimeMillis()
        if (isAutoRefresh && currentTime - lastRefreshTime.get() < minRefreshInterval) {
            Log.d("ChatViewModel", "Skipped loadMessages due to minRefreshInterval")
            return
        }

        viewModelScope.launch {
            if (!isAutoRefresh) _isLoading.value = true
            _errorMessage.value = null

            try {
                val token = getToken(context) ?: run {
                    _errorMessage.value = "Not logged in"
                    Log.w("ChatViewModel", "No token found, cannot load messages")
                    return@launch
                }

                val request = Request.Builder()
                    .url("$baseUrl/campaigns/$id/messages")
                    .header("Authorization", "Bearer $token")
                    .build()

                val result = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() // still on IO
                        object {
                            val isSuccessful = response.isSuccessful
                            val code = response.code
                            val message = response.message
                            val body = bodyString
                        }
                    }
                }

                if (result.isSuccessful && result.body != null) {
                    val messagesResponse = json.decodeFromString<GetMessagesResponse>(result.body)
                    val currentMessages = _messages.value
                    if (messagesResponse.messages != currentMessages) {
                        _messages.value = messagesResponse.messages
                        lastRefreshTime.set(currentTime)
                        Log.d("ChatViewModel", "Loaded ${messagesResponse.messages.size} messages")
                    }
                } else {
                    val errorMsg = "Failed to load messages: ${result.code}"
                    if (!isAutoRefresh) _errorMessage.value = errorMsg
                    Log.e("ChatViewModel", "$errorMsg - ${result.message}")
                }
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.localizedMessage}"
                if (!isAutoRefresh) _errorMessage.value = errorMsg
                Log.e("ChatViewModel", "Error loading messages", e)
            } finally {
                if (!isAutoRefresh) _isLoading.value = false
            }
        }

    }

    fun sendMessage(
        context: Context,
        content: String,
        messageType: MessageType = MessageType.CHAT,
        isToGM: Boolean = false,
        campaignId: String? = null
    ) {
        Log.d("ChatViewModel", "sendMessage(content=$content, type=$messageType, isToGM=$isToGM, campaignId=$campaignId)")
        val id = campaignId ?: currentCampaignId ?: return

        viewModelScope.launch {
            try {
                val token = getToken(context) ?: run {
                    _errorMessage.value = "Not logged in"
                    Log.w("ChatViewModel", "No token found, cannot send message")
                    return@launch
                }

                val sendRequest = SendMessageRequest(
                    campaignId = id,
                    content = content,
                    messageType = messageType,
                    isToGM = isToGM
                )

                val requestBody = json.encodeToString(SendMessageRequest.serializer(), sendRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/campaigns/$id/messages")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string()
                        object {
                            val isSuccessful = response.isSuccessful
                            val code = response.code
                            val message = response.message
                            val body = bodyString
                        }
                    }
                }

                if (result.isSuccessful && result.body != null) {
                    val newMessage = json.decodeFromString<ChatMessage>(result.body)
                    _messages.value = _messages.value + newMessage
                    Log.d("ChatViewModel", "Message sent successfully")
                    delay(500)
                    loadMessages(context, id, isAutoRefresh = true)
                } else {
                    _errorMessage.value = "Failed to send message: ${result.code}"
                    Log.e("ChatViewModel", "Failed to send message: ${result.code} - ${result.message}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }

    fun postRollCommand(command: String, context: Context? = null) {
        Log.d("ChatViewModel", "postRollCommand(command=$command)")
        if (!command.startsWith("/r ")) {
            if (context != null) {
                sendMessage(context, "You: $command", MessageType.CHAT)
            }
            return
        }
        val result = parseAndRollDice(command.removePrefix("/r ").trim())
        if (context != null) {
            sendMessage(context, result, MessageType.ROLL)
        }
    }

    fun postExternalRoll(description: String, context: Context? = null) {
        Log.d("ChatViewModel", "postExternalRoll(description=$description)")
        if (context != null) {
            sendMessage(context, description, MessageType.ROLL)
        }
    }

    fun addMessage(message: String, messageType: MessageType, campaignId: String?, context: Context? = null) {
        Log.d("ChatViewModel", "addMessage(message=$message, type=$messageType, campaignId=$campaignId)")
        Log.d("ChatViewModel", "context is $context")

        val isToGM = message.startsWith("/ToDM")
        val cleanContent = if (isToGM) message.removePrefix("/ToDM ") else message

        if (context != null) {
            sendMessage(context, cleanContent, messageType, isToGM, campaignId)
        }
    }

    private fun getToken(context: Context): String? {
        Log.d("ChatViewModel", "getToken()")
        val prefs = context.getSharedPreferences("dice_app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    override fun onCleared() {
        Log.d("ChatViewModel", "onCleared()")
        super.onCleared()
        stopAutoRefresh()
    }
}
