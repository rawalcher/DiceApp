package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diceapp.viewModels.Ability
import com.example.diceapp.components.AbilityDropdown
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun AddAttackScreen(
    navController: NavController,
    abilities: List<Ability>,
    proficiencyBonus: Int,
    onAddAttack: (name: String, toHit: Int, damageDice: String, damageModifier: Int, damageType: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedToHitAbility by remember { mutableStateOf<Ability?>(null) }
    var selectedToDamageAbility by remember { mutableStateOf<Ability?>(null) }
    var includeProficiency by remember { mutableStateOf(false) }
    var damageDice by remember { mutableStateOf("") }
    var damageType by remember { mutableStateOf("") }

    val toHitModifier = (selectedToHitAbility?.modifier ?: 0) + if (includeProficiency) proficiencyBonus else 0
    val toDamageModifier = selectedToDamageAbility?.modifier ?: 0

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = "Add Attack",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.White) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            // To Hit Section
            Text("To Hit Modifier", color = Color.White)
            AbilityDropdown(
                options = abilities,
                selected = selectedToHitAbility,
                onSelectedChange = { selectedToHitAbility = it },
                label = "To Hit Ability"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeProficiency,
                    onCheckedChange = { includeProficiency = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White)
                )
                Text("Add Proficiency Bonus", color = Color.White)
            }
            Text("Result: ${if (toHitModifier >= 0) "+" else ""}$toHitModifier", color = Color.White)

            // To Damage Section
            Text("To Damage Modifier", color = Color.White)
            AbilityDropdown(
                options = abilities,
                selected = selectedToDamageAbility,
                onSelectedChange = { selectedToDamageAbility = it },
                label = "To Damage Ability"
            )

            Text("Result: ${if (toDamageModifier >= 0) "+" else ""}$toDamageModifier", color = Color.White)

            // Damage Dice
            OutlinedTextField(
                value = damageDice,
                onValueChange = { damageDice = it },
                label = { Text("Damage Dice (e.g. 1d6)", color = Color.White) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            // Damage Type
            OutlinedTextField(
                value = damageType,
                onValueChange = { damageType = it },
                label = { Text("Damage Type (e.g. Slashing)", color = Color.White) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White)
            )

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (name.isNotBlank() && damageDice.isNotBlank() && damageType.isNotBlank()) {
                            onAddAttack(name, toHitModifier, damageDice, toDamageModifier, damageType)
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add", color = Color.White)
                }

                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}
