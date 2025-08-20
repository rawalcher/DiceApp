package com.example.diceapp.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.CampaignViewModel
import com.example.diceapp.models.Campaign

@Composable
fun CampaignsScreen(
    navController: NavController,
    campaignViewModel: CampaignViewModel
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        campaignViewModel.loadCampaigns(context)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campaigns",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Create Campaign")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            if (campaignViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(campaignViewModel.campaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            onJoin = {
                                if (campaign.isJoined) {
                                    campaignViewModel.selectCampaign(campaign)
                                    navController.navigate("chat/${campaign.id}")
                                } else {
                                    campaignViewModel.joinCampaign(context, campaign.id) {
                                        campaignViewModel.selectCampaign(campaign)
                                        navController.navigate("chat/${campaign.id}")
                                    }
                                }
                            },
                            onDelete = { campaignViewModel.deleteCampaign(context, campaign.id) }
                        )
                    }
                }
            }

            campaignViewModel.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Show error message
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCampaignDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, maxPlayers ->
                campaignViewModel.createCampaign(context, name, description, maxPlayers) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun CampaignCard(
    campaign: Campaign,
    onJoin: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Color.White, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "by ${campaign.ownerName}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = campaign.description,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Players: ${campaign.playerCount}/${campaign.maxPlayers}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onJoin,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (campaign.isJoined) Color.Green else Color.White,
                        contentColor = if (campaign.isJoined) Color.White else Color.Black
                    )
                ) {
                    Text(if (campaign.isJoined) "Enter Campaign" else "Join Campaign")
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun CreateCampaignDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, maxPlayers: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableStateOf("6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Campaign") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Campaign Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = maxPlayers,
                    onValueChange = { if (it.all { char -> char.isDigit() }) maxPlayers = it },
                    label = { Text("Max Players") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val players = maxPlayers.toIntOrNull() ?: 6
                    onConfirm(name, description, players)
                },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}