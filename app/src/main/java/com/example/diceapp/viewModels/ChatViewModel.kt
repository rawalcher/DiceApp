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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    var isLoading by mutableStateOf(false)
        private set

    var currentCampaignId by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun setCampaign(campaignId: String) {
        currentCampaignId = campaignId
        _messages.value = emptyList()
    }

    fun loadMessages(context: Context, campaignId: String? = null) {
        val id = campaignId ?: currentCampaignId ?: return

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val request = Request.Builder()
                    .url("$baseUrl/campaigns/$id/messages")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val messagesResponse =
                            json.decodeFromString<GetMessagesResponse>(responseBody)
                        _messages.value = messagesResponse.messages
                        Log.d("ChatViewModel", "Loaded ${messagesResponse.messages.size} messages")
                    }
                } else {
                    errorMessage = "Failed to load messages: ${response.code}"
                    Log.e(
                        "ChatViewModel",
                        "Failed to load messages: ${response.code} - ${response.message}"
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e("ChatViewModel", "Error loading messages", e)
            } finally {
                isLoading = false
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
        val id = campaignId ?: currentCampaignId ?: return

        viewModelScope.launch {
            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val sendRequest = SendMessageRequest(
                    campaignId = id,
                    content = content,
                    messageType = messageType,
                    isToGM = isToGM
                )

                val requestBody = json.encodeToString(sendRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/campaigns/$id/messages")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newMessage = json.decodeFromString<ChatMessage>(responseBody)
                        _messages.value = _messages.value + newMessage
                        Log.d("ChatViewModel", "Message sent successfully")
                    }
                } else {
                    errorMessage = "Failed to send message: ${response.code}"
                    Log.e(
                        "ChatViewModel",
                        "Failed to send message: ${response.code} - ${response.message}"
                    )
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e("ChatViewModel", "Error sending message", e)
            }
        }
    }

    // TODO: remove
    fun postRollCommand(command: String, context: Context? = null) {
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

    fun postMessage(text: String, context: Context? = null) {
        if (context != null) {
            sendMessage(context, "You: $text", MessageType.CHAT)
        }
    }

    fun postExternalRoll(description: String, context: Context? = null) {
        if (context != null) {
            sendMessage(context, description, MessageType.ROLL)
        }
    }

    fun addMessage(message: String, context: Context? = null) {
        // Determine message type based on content
        val messageType =
            if (message.contains("🎲") || message.contains("⚔️") || message.startsWith("/ToDM")) {
                MessageType.ROLL
            } else {
                MessageType.CHAT
            }

        val isToGM = message.startsWith("/ToDM")
        val cleanContent = if (isToGM) message.removePrefix("/ToDM ") else message

        if (context != null) {
            sendMessage(context, cleanContent, messageType, isToGM)
        }
    }

    fun getFilteredMessages(messageType: MessageType): List<ChatMessage> {
        return _messages.value.filter { it.messageType == messageType }
    }

    fun getChatMessages(): List<ChatMessage> = getFilteredMessages(MessageType.CHAT)
    fun getRollMessages(): List<ChatMessage> = getFilteredMessages(MessageType.ROLL)

    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("dice_app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }
}