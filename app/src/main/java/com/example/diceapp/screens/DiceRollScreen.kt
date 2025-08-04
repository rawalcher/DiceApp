package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.ViewModels.ChatViewModel
import com.example.diceapp.ViewModels.DiceRollViewModel
import com.example.diceapp.ViewModels.DiceRollViewModel.MessageMode
import com.example.diceapp.ViewModels.DiceRollViewModel.RollMode

@Composable
fun DiceRollScreen(
    rollLabel: String,
    modifier: Int,
    proficiencyBonus: Int? = null,
    navController: NavController,
    chatViewModel: ChatViewModel,
    diceRollViewModel: DiceRollViewModel
) {
    Scaffold(
        topBar = {
            MessageModeToggle(
                selected = diceRollViewModel.messageMode,
                onSelect = { diceRollViewModel.messageMode = it }
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

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Modifier: ${if (modifier >= 0) "+" else ""}$modifier",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            proficiencyBonus?.takeIf { it > 0 }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Proficiency Bonus: +$it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = {
                    val message = diceRollViewModel.rollAndFormatMessage(
                        label = rollLabel,
                        modifier = modifier,
                        proficiencyBonus = proficiencyBonus
                    )
                    chatViewModel.postExternalRoll(message)
                    navController.navigate("chat") { popUpTo("menu") { inclusive = false } }
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
