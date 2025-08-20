package com.example.diceapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diceapp.models.ChatMessage
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatComponent(
    chatViewModel: ChatViewModel,
    messageType: MessageType,
    showInput: Boolean = true,
    inputPlaceholder: String = "Type a message...",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val filteredMessages = messages.filter { it.messageType == messageType }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(chatViewModel.currentCampaignId) {
        chatViewModel.currentCampaignId?.let { campaignId ->
            chatViewModel.loadMessages(context, campaignId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMessages.reversed()) { message ->
                MessageItem(
                    message = message,
                    messageType = messageType
                )
            }
        }

        if (showInput) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text(inputPlaceholder) },
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            when (messageType) {
                                MessageType.CHAT -> {
                                    chatViewModel.sendMessage(context, inputText, MessageType.CHAT)
                                }
                                MessageType.ROLL -> {
                                    if (inputText.startsWith("/r ")) {
                                        chatViewModel.postRollCommand(inputText, context)
                                    } else {
                                        chatViewModel.sendMessage(context, inputText, MessageType.ROLL)
                                    }
                                }
                            }
                            inputText = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }

    if (chatViewModel.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    chatViewModel.errorMessage?.let { error ->
        LaunchedEffect(error) {
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    messageType: MessageType
) {
    val dateFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = dateFormatter.format(Date(message.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                message.isToGM -> Color(0xFF3F51B5).copy(alpha = 0.1f) // Blue tint for GM messages
                messageType == MessageType.ROLL -> Color(0xFF4CAF50).copy(alpha = 0.1f) // Green tint for rolls
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (message.isToGM) {
                        Text(
                            text = " (to GM)",
                            fontSize = 12.sp,
                            color = Color(0xFF3F51B5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = timeString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (messageType) {
                MessageType.ROLL -> {
                    RollMessageContent(message.content)
                }
                MessageType.CHAT -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RollMessageContent(content: String) {
    val lines = content.split("\n")

    Column {
        lines.forEachIndexed { index, line ->
            when {
                line.matches(Regex("""= \d+""")) -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (index < lines.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}