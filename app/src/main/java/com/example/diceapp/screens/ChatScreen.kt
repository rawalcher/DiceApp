package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.diceapp.components.ChatComponent
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    campaignId: String? = null,
    autoRefreshEnabled: Boolean = true,
    refreshIntervalMs: Long = 5000L
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAutoRefreshActive by remember { mutableStateOf(autoRefreshEnabled) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    val tabs = remember { listOf("Chat", "Rolls") }

    // Set campaign ID if provided
    LaunchedEffect(campaignId) {
        campaignId?.let {
            chatViewModel.setCampaign(it)
            // Load initial messages
            chatViewModel.loadMessages(context, it)
        }
    }

    // Auto-refresh effect
    LaunchedEffect(chatViewModel.currentCampaignId, isAutoRefreshActive) {
        if (isAutoRefreshActive && chatViewModel.currentCampaignId != null) {
            while (isAutoRefreshActive) {
                delay(refreshIntervalMs)
                if (isAutoRefreshActive && chatViewModel.currentCampaignId != null) {
                    chatViewModel.loadMessages(context, chatViewModel.currentCampaignId)
                }
            }
        }
    }

    // Stop auto-refresh when screen is not visible or app goes to background
    DisposableEffect(Unit) {
        onDispose {
            isAutoRefreshActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Chat") },
                actions = {
                    // Manual refresh button
                    IconButton(
                        onClick = {
                            chatViewModel.currentCampaignId?.let { id ->
                                isManualRefreshing = true
                                chatViewModel.loadMessages(context, id)
                                // Reset the refreshing state after a short delay
                                scope.launch {
                                    delay(1000)
                                    isManualRefreshing = false
                                }
                            }
                        },
                        enabled = !isManualRefreshing
                    ) {
                        if (isManualRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row with better styling
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

            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> ChatTab(chatViewModel)
                1 -> RollsTab(chatViewModel)
            }
        }
    }
}

@Composable
private fun ChatTab(chatViewModel: ChatViewModel) {
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

@Composable
private fun RollsTab(chatViewModel: ChatViewModel) {
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