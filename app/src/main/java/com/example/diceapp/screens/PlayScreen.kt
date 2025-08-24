package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.components.ChatComponent
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel,
    navController: NavController,
    campaignId: String? = null,
    autoRefreshEnabled: Boolean = true,
    refreshIntervalMs: Long = 5000L
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isAutoRefreshActive by remember { mutableStateOf(autoRefreshEnabled) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    val tabs = remember { listOf("Chat", "Rolls", "Actions") }

    LaunchedEffect(campaignId) {
        campaignId?.let {
            chatViewModel.setCampaign(it)
            chatViewModel.loadMessages(context, it)
            characterViewModel.loadCharacterForCampaign(context, it)
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
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Back"
                        )
                    }
                },
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
                2 -> ActionsTab(navController = navController)
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
private fun ActionsTab(navController: NavController) {
    val actionButtons = remember {
        listOf(
            ActionButtonData("Stats", Icons.Default.Star, "View ability scores") {
                navController.navigate("stats")
            },
            ActionButtonData("Saving Throws", Icons.Default.Warning, "Roll saving throws") {
                navController.navigate("saving_throws")
            },
            ActionButtonData("Skills", Icons.AutoMirrored.Filled.List, "View and roll skills") {
                navController.navigate("skills")
            },
            ActionButtonData("Combat Stats", Icons.Default.Favorite, "HP, AC, and more") {
                navController.navigate("combat_stats")
            },
            ActionButtonData("Attacks", Icons.Default.PlayArrow, "Manage attacks") {
                navController.navigate("attack")
            },
            ActionButtonData("Spells", Icons.Default.Star, "Manage spellbook") {
                navController.navigate("spells")
            },
            ActionButtonData("Resources", Icons.Default.Settings, "Additional resources") {
                navController.navigate("resources")
            },
            ActionButtonData("Modifiers", Icons.Default.Add, "Additional modifiers") {
                navController.navigate("modifiers")
            },
            ActionButtonData("DM Level Up", Icons.Default.KeyboardArrowUp, "Level up players") {
                navController.navigate("dm_level_up")
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