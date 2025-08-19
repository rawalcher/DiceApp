package com.example.diceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.AppliesTo
import com.example.diceapp.viewModels.ModifierViewModel
import com.example.diceapp.viewModels.RollModifier

@Composable
fun AddModifierScreen(
    navController: NavController,
    modifierViewModel: ModifierViewModel
) {
    var name by remember { mutableStateOf("") }
    var flatText by remember { mutableStateOf("") }
    var extraDice by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(AppliesTo.ALL) }

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Add Modifier",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name (e.g., Bless)", color = Color.White) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            OutlinedTextField(
                value = flatText,
                onValueChange = { new ->
                    val cleaned = new.trim()
                    if (cleaned.matches(Regex("^-?\\d{0,3}\$"))) flatText = cleaned
                },
                label = { Text("Flat Bonus (e.g., +1 / -1)", color = Color.White) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            OutlinedTextField(
                value = extraDice,
                onValueChange = { extraDice = it.filter { ch -> ch.isLetterOrDigit() || ch == 'd' || ch == 'D' }.lowercase() },
                label = { Text("Extra Dice (e.g., 1d4)", color = Color.White) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            // Scope chips
            Text("Applies To", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val Chip: @Composable (Boolean, String, () -> Unit) -> Unit = { selected, text, onClick ->
                    if (selected) Button(onClick = onClick) { Text(text) }
                    else OutlinedButton(onClick = onClick) { Text(text) }
                }
                Chip(scope == AppliesTo.ALL, "All") { scope = AppliesTo.ALL }
                Chip(scope == AppliesTo.ATTACK, "Attack") { scope = AppliesTo.ATTACK }
                Chip(scope == AppliesTo.SAVE, "Save") { scope = AppliesTo.SAVE }
                Chip(scope == AppliesTo.CHECK, "Check") { scope = AppliesTo.CHECK }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val flat = flatText.toIntOrNull() ?: 0
                            val dice = extraDice.ifBlank { null }
                            modifierViewModel.addModifier(
                                RollModifier(
                                    name = name.trim(),
                                    flatBonus = flat,
                                    extraDice = dice,
                                    appliesTo = scope
                                )
                            )
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) { Text("Add", color = Color.White) }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel", color = Color.White) }
            }
        }
    }
}
