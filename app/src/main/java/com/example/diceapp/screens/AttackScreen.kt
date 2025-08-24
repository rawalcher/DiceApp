package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.diceapp.models.MessageType
import com.example.diceapp.models.LocalCampaignSelection

@Composable
fun AttackScreen(
    navController: NavController,
    attackViewModel: AttackViewModel,
    chatViewModel: ChatViewModel,
    diceRollViewModel: DiceRollViewModel
) {
    val attacks = attackViewModel.attacks
    val currentCampaignId = LocalCampaignSelection.current.id
    val context = LocalContext.current
    var isRemoveMode by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { isRemoveMode = !isRemoveMode }
                        .background(Color.Red, shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", color = Color.White, fontSize = 20.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { navController.navigate("add_attack") }
                        .background(Color(0xFF4CAF50), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", color = Color.White, fontSize = 20.sp)
                }
            }

            // Column labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Name", modifier = Modifier.weight(1f), color = Color.White, fontSize = 30.sp)
                Text("Atk", modifier = Modifier.weight(0.6f), color = Color.White, fontSize = 30.sp)
                Text("Damage", modifier = Modifier.weight(1f), color = Color.White, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attack rows
            attacks.forEachIndexed { index, attack ->
                AttackRow(
                    name = attack.name,
                    toHitModifier = attack.toHit,
                    damageDice = attack.damageDice,
                    damageModifier = attack.damageModifier,
                    damageType = attack.damageType,
                    onAttackClick = {
                        val encoded = URLEncoder.encode(attack.name, StandardCharsets.UTF_8.toString())
                        navController.navigate("dice_roll/$encoded/${attack.toHit}/0/Attack")
                    },
                    onDamageClick = {
                        val message = diceRollViewModel.rollDamage(
                            label = attack.name,
                            damageDice = attack.damageDice,
                            damageModifier = attack.damageModifier,
                            damageType = attack.damageType
                        )
                        chatViewModel.addMessage(message, MessageType.ROLL, currentCampaignId, context)
                        navController.navigate("chat")
                    },
                    showRemoveIcon = isRemoveMode,
                    onRemoveClick = {
                        attackViewModel.removeAttack(index)
                        isRemoveMode = false
                    }
                )
            }
        }
    }
}

@Composable
fun AttackRow(
    name: String,
    toHitModifier: Int,
    damageDice: String,
    damageModifier: Int,
    damageType: String,
    onAttackClick: () -> Unit,
    onDamageClick: () -> Unit,
    showRemoveIcon: Boolean = false,
    onRemoveClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showRemoveIcon) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onRemoveClick() }
                        .background(Color.Red, shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", color = Color.White, fontSize = 20.sp)
                }
            }

            Text(
                text = name,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAttackClick() },
                color = Color.White,
                fontSize = 20.sp
            )
            Text(
                text = if (toHitModifier >= 0) "+$toHitModifier" else "$toHitModifier",
                modifier = Modifier.weight(0.6f),
                color = Color.White,
                fontSize = 20.sp
            )
            Text(
                text = "$damageDice ${if (damageModifier >= 0) "+$damageModifier" else "$damageModifier"}\n$damageType",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDamageClick() },
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}
