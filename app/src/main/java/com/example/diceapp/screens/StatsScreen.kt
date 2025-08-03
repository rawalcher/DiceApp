package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.diceapp.ViewModels.Ability
import com.example.diceapp.ViewModels.CharacterViewModel
import com.example.diceapp.ViewModels.ChatViewModel
import kotlin.random.Random

@Composable
fun StatsScreen(
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel
) {
    val abilities = characterViewModel.abilities

    Scaffold{ padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Layout: 2 columns × 3 rows
            abilities.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Each row takes equal height
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { ability ->
                        StatCard(
                            ability = ability,
                            onRoll = {
                                val dieRoll = Random.nextInt(1, 21)
                                val total = dieRoll + ability.modifier
                                chatViewModel.postExternalRoll(
                                    "\uD83C\uDFB2 ${ability.name} Check: Rolled 1d20 ($dieRoll) + ${ability.modifier}\n= $total"
                                )

                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // If there's only 1 item in the row (odd count), add a spacer
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
@Composable
fun StatCard(ability: Ability, onRoll: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ability.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text("Value: ${ability.value}")
            Text("Modifier: ${if (ability.modifier >= 0) "+" else ""}${ability.modifier}")
            Button(onClick = onRoll) {
                Text("Roll")
            }
        }
    }
}
