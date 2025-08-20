package com.example.diceapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    CHAT, ROLL
}

@Serializable
data class ChatMessage(
    val id: String,
    val campaignId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val messageType: MessageType,
    val timestamp: Long,
    val isToGM: Boolean = false
)

@Serializable
data class SendMessageRequest(
    val campaignId: String,
    val content: String,
    val messageType: MessageType,
    val isToGM: Boolean = false
)

@Serializable
data class GetMessagesResponse(
    val messages: List<ChatMessage>
)