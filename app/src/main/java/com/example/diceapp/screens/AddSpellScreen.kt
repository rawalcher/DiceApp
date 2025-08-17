package com.example.diceapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.components.AbilityDropdown
import com.example.diceapp.viewModels.Ability

enum class SpellKind { ATTACK, SAVE, UTILITY }

@Composable
fun AddSpellScreen(
    navController: NavController,
    abilities: List<Ability>,
    proficiencyBonus: Int,
    spellSaveDC: Int,
    onAddSpell: (
        name: String,
        level: Int,
        castingTime: String,
        range: String,
        components: String,
        concentration: Boolean,
        ritual: Boolean,
        attackBonus: Int?,
        saveDC: Int?,
        saveAbility: String?,
        damageDice: String?,
        damageModifier: Int,
        damageType: String?,
        description: String
    ) -> Unit
) {
    // Core fields
    var name by remember { mutableStateOf("") }
    var levelText by remember { mutableStateOf("0") }
    var castingTime by remember { mutableStateOf("1 action") }
    var range by remember { mutableStateOf("60 feet") }
    var components by remember { mutableStateOf("V, S") }
    var description by remember { mutableStateOf("") }
    var concentration by remember { mutableStateOf(false) }
    var ritual by remember { mutableStateOf(false) }

    // Spell kind
    var kind by remember { mutableStateOf(SpellKind.ATTACK) }

    // ATTACK
    var spellcastingAbility by remember { mutableStateOf<Ability?>(null) }
    var includeProfForAtk by remember { mutableStateOf(true) }
    val atkBonus = (spellcastingAbility?.modifier ?: 0) + if (includeProfForAtk) proficiencyBonus else 0

    // SAVE
    val saveAbilities = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
    var selectedSaveAbility by remember { mutableStateOf("DEX") }

    // DAMAGE
    var damageDice by remember { mutableStateOf("") }
    var damageType by remember { mutableStateOf("") }
    var damageAbility by remember { mutableStateOf<Ability?>(null) }
    val damageModifier = damageAbility?.modifier ?: 0

    Scaffold(containerColor = Color.Black) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Add Spell",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Level
            item {
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { new -> levelText = new.filter { it.isDigit() }.take(2) },
                    label = { Text("Level (0–9)", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Casting Time
            item {
                OutlinedTextField(
                    value = castingTime,
                    onValueChange = { castingTime = it },
                    label = { Text("Casting Time", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Range
            item {
                OutlinedTextField(
                    value = range,
                    onValueChange = { range = it },
                    label = { Text("Range", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Components
            item {
                OutlinedTextField(
                    value = components,
                    onValueChange = { components = it },
                    label = { Text("Components (e.g., V, S, M)", color = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Description (multiline)
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color.White) },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().background(Color.Black),
                    textStyle = LocalTextStyle.current.copy(color = Color.White)
                )
            }

            // Toggles
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = concentration,
                            onCheckedChange = { concentration = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White)
                        )
                        Text("Concentration", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = ritual,
                            onCheckedChange = { ritual = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White)
                        )
                        Text("Ritual", color = Color.White)
                    }
                }
            }

            item {
                val Chip: @Composable (Boolean, String, () -> Unit) -> Unit = { selected, text, onClick ->
                    if (selected) Button(onClick = onClick) { Text(text) }
                    else OutlinedButton(onClick = onClick) { Text(text) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(kind == SpellKind.ATTACK, "Attack") { kind = SpellKind.ATTACK }
                    Chip(kind == SpellKind.SAVE,   "Save")   { kind = SpellKind.SAVE }
                    Chip(kind == SpellKind.UTILITY,"Utility"){ kind = SpellKind.UTILITY }
                }
            }
            if (kind == SpellKind.ATTACK) {
                item {
                    Text("Spellcasting Ability", color = Color.White)
                    AbilityDropdown(
                        options = abilities,
                        selected = spellcastingAbility,
                        onSelectedChange = { spellcastingAbility = it },
                        label = "Spellcasting Ability"
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeProfForAtk,
                            onCheckedChange = { includeProfForAtk = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White)
                        )
                        Text("Include Proficiency", color = Color.White)
                    }
                    Text("Spell Attack Bonus: ${if (atkBonus >= 0) "+" else ""}$atkBonus", color = Color.White)
                }
            }
            if (kind == SpellKind.SAVE) {
                item { Text("Target Save Ability", color = Color.White) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            saveAbilities.take(3).forEach { abbr ->
                                val Chip: @Composable (Boolean, String, () -> Unit) -> Unit = { selected, text, onClick ->
                                    if (selected) Button(onClick = onClick) { Text(text) }
                                    else OutlinedButton(onClick = onClick) { Text(text) }
                                }
                                Chip(selectedSaveAbility == abbr, abbr) { selectedSaveAbility = abbr }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            saveAbilities.drop(3).forEach { abbr ->
                                val Chip: @Composable (Boolean, String, () -> Unit) -> Unit = { selected, text, onClick ->
                                    if (selected) Button(onClick = onClick) { Text(text) }
                                    else OutlinedButton(onClick = onClick) { Text(text) }
                                }
                                Chip(selectedSaveAbility == abbr, abbr) { selectedSaveAbility = abbr }
                            }
                        }
                    }
                }
                item { Text("Save DC: $spellSaveDC", color = Color.White) }
            }
            if (kind != SpellKind.UTILITY) {
                item { Text("Damage (optional)", color = Color.White) }
                item {
                    OutlinedTextField(
                        value = damageDice,
                        onValueChange = { damageDice = it },
                        label = { Text("Damage Dice (e.g. 2d8)", color = Color.White) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().background(Color.Black),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                }
                item {
                    OutlinedTextField(
                        value = damageType,
                        onValueChange = { damageType = it },
                        label = { Text("Damage Type (e.g. Fire)", color = Color.White) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().background(Color.Black),
                        textStyle = LocalTextStyle.current.copy(color = Color.White)
                    )
                }
                item {
                    Text("Damage Ability Modifier (optional)", color = Color.White)
                    AbilityDropdown(
                        options = abilities,
                        selected = damageAbility,
                        onSelectedChange = { damageAbility = it },
                        label = "Damage Ability"
                    )
                    Text("Result: ${if (damageModifier >= 0) "+" else ""}$damageModifier", color = Color.White)
                }
            }

            // Actions
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val level = levelText.toIntOrNull()?.coerceIn(0, 9) ?: 0

                            val attackBonusValue = if (kind == SpellKind.ATTACK) atkBonus else null
                            val saveDCValue = if (kind == SpellKind.SAVE) spellSaveDC else null
                            val saveAbilityValue = if (kind == SpellKind.SAVE) selectedSaveAbility else null

                            val includeDamage = kind != SpellKind.UTILITY && damageDice.isNotBlank() && damageType.isNotBlank()
                            val damageDiceValue = if (includeDamage) damageDice else null
                            val damageTypeValue = if (includeDamage) damageType else null
                            val damageModValue = if (includeDamage) damageModifier else 0

                            if (name.isNotBlank() && description.isNotBlank()) {
                                onAddSpell(
                                    name,
                                    level,
                                    castingTime,
                                    range,
                                    components,
                                    concentration,
                                    ritual,
                                    attackBonusValue,
                                    saveDCValue,
                                    saveAbilityValue,
                                    damageDiceValue,
                                    damageModValue,
                                    damageTypeValue,
                                    description
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
}
