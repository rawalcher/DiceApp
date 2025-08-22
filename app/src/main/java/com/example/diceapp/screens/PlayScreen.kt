package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diceapp.components.ChatComponent
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    chatViewModel: ChatViewModel,
    campaignId: String? = null,
    autoRefreshEnabled: Boolean = true,
    refreshIntervalMs: Long = 5000L,
    onNavigateToCharacterSheet: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSpells: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiceRoller: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAutoRefreshActive by remember { mutableStateOf(autoRefreshEnabled) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    val tabs = remember { listOf("Chat", "Rolls", "Actions") }

    LaunchedEffect(campaignId) {
        campaignId?.let {
            chatViewModel.setCampaign(it)

            chatViewModel.loadMessages(context, it)
        }
    }

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

    DisposableEffect(Unit) {
        onDispose {
            isAutoRefreshActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Play") },
                actions = {
                    IconButton(
                        onClick = {
                            chatViewModel.currentCampaignId?.let { id ->
                                isManualRefreshing = true
                                chatViewModel.loadMessages(context, id)
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
                0 -> ChatTab(chatViewModel)
                1 -> RollsTab(chatViewModel)
                2 -> ActionsTab(
                    onNavigateToCharacterSheet = onNavigateToCharacterSheet,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToSpells = onNavigateToSpells,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDiceRoller = onNavigateToDiceRoller,
                    onNavigateToNotes = onNavigateToNotes
                )
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

@Composable
private fun ActionsTab(
    onNavigateToCharacterSheet: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSpells: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiceRoller: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {}
) {
    val actionButtons = remember {
        // TODO: actually link screens
        listOf(
            ActionButtonData("Character Sheet", Icons.Default.Person, "View character details") {
                onNavigateToCharacterSheet()
            },
            ActionButtonData("Inventory", Icons.Default.List, "Manage items") {
                onNavigateToInventory()
            },
            ActionButtonData("Spells", Icons.Default.Star, "View spellbook") {
                onNavigateToSpells()
            },
            ActionButtonData("Dice Roller", Icons.Default.PlayArrow, "Advanced dice rolling") {
                onNavigateToDiceRoller()
            },
            ActionButtonData("Notes", Icons.Default.Edit, "Campaign notes") {
                onNavigateToNotes()
            },
            ActionButtonData("Settings", Icons.Default.Settings, "App settings") {
                onNavigateToSettings()
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(actionButtons) { buttonData ->
            ActionButton(
                data = buttonData,
                onClick = buttonData.onClick
            )
        }
    }
}

@Composable
private fun ActionButton(
    data: ActionButtonData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = data.description,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class ActionButtonData(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val onClick: () -> Unit
)