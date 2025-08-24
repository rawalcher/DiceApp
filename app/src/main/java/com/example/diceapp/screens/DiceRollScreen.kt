package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.viewModels.DiceRollViewModel.MessageMode
import com.example.diceapp.viewModels.DiceRollViewModel.RollMode
import com.example.diceapp.viewModels.ModifierViewModel

@Composable
fun DiceRollScreen(
    rollLabel: String,
    modifier: Int,
    proficiencyBonus: Int? = null,
    type: String,
    navController: NavController,
    chatViewModel: ChatViewModel,
    diceRollViewModel: DiceRollViewModel,
    modifierViewModel: ModifierViewModel
) {
    val currentContext = LocalContext.current
    val (extraFlat, extraDice) = modifierViewModel.aggregateFor(type)
    Scaffold(
        topBar = {
            MessageModeToggle(
                selected = diceRollViewModel.messageMode,
                onSelect = { diceRollViewModel.messageMode = it },
            )
        },
        bottomBar = {
            RollModeToggle(
                selected = diceRollViewModel.rollMode,
                onSelect = { diceRollViewModel.rollMode = it }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rollLabel,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildString {
                    append("Modifier: $modifier")
                    if (proficiencyBonus != null && proficiencyBonus > 0) {
                        append(" | Proficiency: $proficiencyBonus")
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (extraFlat != 0 || extraDice.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("Buffs: ")
                        val parts = mutableListOf<String>()
                        if (extraFlat != 0) parts += (if (extraFlat > 0) "+$extraFlat" else "$extraFlat")
                        if (extraDice.isNotEmpty()) parts += extraDice.joinToString(" + ")
                        append(parts.joinToString(" "))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val message = diceRollViewModel.rollAndFormatMessage(
                        label = rollLabel,
                        modifier = modifier,
                        proficiencyBonus = proficiencyBonus,
                        type = type,
                        extraFlatModifier = extraFlat,
                        extraDice = extraDice
                    )
                    chatViewModel.postExternalRoll(message, currentContext)
                    navController.navigate("chat") {
                        popUpTo("menu") { inclusive = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Roll")
            }
        }
    }
}

@Composable
private fun MessageModeToggle(
    selected: MessageMode,
    onSelect: (MessageMode) -> Unit
) {
    val options = listOf(MessageMode.ToDM, MessageMode.Public)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { mode ->
            val label = when (mode) {
                MessageMode.ToDM -> "To DM"
                MessageMode.Public -> "Public"
            }
            val isSelected = selected == mode
            Button(
                onClick = { onSelect(mode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.White else Color.Black,
                    contentColor = if (isSelected) Color.Black else Color.White
                ),
                border = if (!isSelected) BorderStroke(2.dp, Color.White) else null
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun RollModeToggle(
    selected: RollMode,
    onSelect: (RollMode) -> Unit
) {
    val options = listOf(RollMode.Advantage, RollMode.Normal, RollMode.Disadvantage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { mode ->
            val label = when (mode) {
                RollMode.Advantage -> "Advantage"
                RollMode.Normal -> "Normal"
                RollMode.Disadvantage -> "Disadvantage"
            }
            val isSelected = selected == mode
            Button(
                onClick = { onSelect(mode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color.White else Color.Black,
                    contentColor = if (isSelected) Color.Black else Color.White
                ),
                border = if (!isSelected) BorderStroke(2.dp, Color.White) else null
            ) {
                Text(label)
            }
        }
    }
}
