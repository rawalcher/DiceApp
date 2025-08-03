package com.example.diceapp.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.sp
import com.example.diceapp.ViewModels.Ability
import com.example.diceapp.ViewModels.CharacterViewModel
import com.example.diceapp.ViewModels.ChatViewModel
import kotlin.random.Random
import androidx.compose.runtime.*
import com.example.diceapp.components.*



@Composable
fun StatsScreen(
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel
) {
    val abilities = characterViewModel.abilities
    var messageMode by remember { mutableStateOf("Public") }
    var rollMode by remember { mutableStateOf("Normal") }

    Scaffold{ padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MessageModeToggle(
                selected = messageMode,
                onSelect = { messageMode = it }
            )

            abilities.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { ability ->
                        StatCard(
                            ability = ability,
                            onRoll = {
                                val roll1 = Random.nextInt(1, 21)
                                val roll2 = Random.nextInt(1, 21)

                                val (finalRoll, rollLabel) = when (rollMode) {
                                    "Advantage" -> maxOf(roll1, roll2) to "($roll1, $roll2) Adv."
                                    "Disadvantage" -> minOf(roll1, roll2) to "($roll1, $roll2) Dis."
                                    else -> roll1 to "($roll1)"
                                }

                                val total = finalRoll + ability.modifier
                                val prefix = if (messageMode == "To DM") "/ToDM " else ""

                                chatViewModel.postExternalRoll(
                                    "$prefix\uD83C\uDFB2 ${ability.name} Check: Rolled 1d20 $rollLabel + ${ability.modifier}\n= $total"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            RollModeToggle(
                selected = rollMode,
                onSelect = { rollMode = it }
            )
        }
    }
}


@Composable
fun StatCard(ability: Ability, onRoll: () -> Unit, modifier: Modifier = Modifier) {
    val cornerRadius = 16.dp
    val shape: Shape = RoundedCornerShape(cornerRadius)

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .border(2.dp, Color.White, shape) // white border with rounded corners
            .clickable { onRoll() },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = ability.name,
                fontSize = 26.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${ability.value}",
                fontSize = 60.sp,
                color = Color.White,
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "${if (ability.modifier >= 0) "+" else ""}${ability.modifier}",
                fontSize = 48.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}

