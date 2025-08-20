package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diceapp.components.ChatComponent
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    campaignId: String? = null
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Rolls")

    // Set campaign ID if provided
    LaunchedEffect(campaignId) {
        campaignId?.let { chatViewModel.setCampaign(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Chat") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    ChatComponent(
                        chatViewModel = chatViewModel,
                        messageType = MessageType.CHAT,
                        showInput = true,
                        inputPlaceholder = "Type a message...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    )
                }

                1 -> {
                    // Rolls tab
                    ChatComponent(
                        chatViewModel = chatViewModel,
                        messageType = MessageType.ROLL,
                        showInput = true,
                        inputPlaceholder = "Type /r 1d20+5 or a roll description...",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}