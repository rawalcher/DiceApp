package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.components.EditDialog
import com.example.diceapp.viewModels.ResourceViewModel
import com.example.diceapp.viewModels.SpellViewModel


@Composable
fun CombatStatsScreen(
    characterViewModel: CharacterViewModel,
    chatViewModel: ChatViewModel,
    diceRollViewModel: DiceRollViewModel,
    resourceViewModel: ResourceViewModel,
    spellViewModel: SpellViewModel,
    onNavigateToChat: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = Color.White
    val cardColor = Color.Black
    val textColor = Color.White
    val successes by characterViewModel.deathSaveSuccesses
    val failures by characterViewModel.deathSaveFailures

    var showEditDialogFor by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Proficiency Bonus", "${characterViewModel.proficiencyBonus}", shape, borderColor, cardColor, textColor, Modifier.weight(1f))
                StatCard("Passive Perception", characterViewModel.passivePerception.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
            }

            // Row 2
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    "Armor Class",
                    characterViewModel.armorClass.toString(),
                    shape, borderColor, cardColor, textColor,
                    Modifier.weight(1f).clickable { showEditDialogFor = "AC" }
                )
                StatCard(
                    "Max HP",
                    characterViewModel.maxHP.toString(),
                    shape, borderColor, cardColor, textColor,
                    Modifier.weight(1f).clickable { showEditDialogFor = "MaxHP" }
                )
                StatCard(
                    "Speed",
                    characterViewModel.speed.toString(),
                    shape, borderColor, cardColor, textColor,
                    Modifier.weight(1f).clickable { showEditDialogFor = "Speed" }
                )
            }

            // Row 3
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    "Current HP",
                    characterViewModel.currentHP.toString(),
                    shape, borderColor, cardColor, textColor,
                    Modifier.weight(1f).clickable { showEditDialogFor = "CurrentHP" }
                )
                StatCard(
                    "Temp HP",
                    characterViewModel.tempHP.toString(),
                    shape, borderColor, cardColor, textColor,
                    Modifier.weight(1f).clickable { showEditDialogFor = "TempHP" }
                )
            }

            // Row 4
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    title = "Initiative",
                    value = characterViewModel.initiative.toString(),
                    shape = shape,
                    borderColor = borderColor,
                    cardColor = cardColor,
                    textColor = textColor,
                    modifier = Modifier.weight(1f).clickable {
                        val message = diceRollViewModel.rollAndFormatMessage(
                            label = "Initiative",
                            modifier = characterViewModel.initiative,
                            type = "Roll"
                        )
                        chatViewModel.addMessage(message)
                        onNavigateToChat()
                    }
                )
                Card(
                    shape = shape,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .border(BorderStroke(2.dp, borderColor), shape),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("INSPIRATION", fontSize = 12.sp, color = textColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        RadioButton(
                            selected = characterViewModel.inspiration,
                            onClick = { characterViewModel.inspiration = !characterViewModel.inspiration },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.Gray,
                                disabledSelectedColor = Color.DarkGray,
                                disabledUnselectedColor = Color.DarkGray
                            )
                        )
                    }
                }
            }

            // Row 5
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = shape,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .border(BorderStroke(2.dp, borderColor), shape),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Decrement Hit Dice
                        Text(
                            text = "−",
                            fontSize = 28.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clickable { characterViewModel.decrementHitDice() }
                                .padding(12.dp)
                        )

                        // Center area — ROLL HIT DIE on click
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val conMod = characterViewModel.abilities.find { it.name == "Constitution" }?.modifier ?: 0
                                    val message = diceRollViewModel.rollHitDice(
                                        hitDieType = characterViewModel.hitDieType,
                                        conModifier = conMod
                                    )
                                    chatViewModel.addMessage(message)
                                    onNavigateToChat()
                                }
                        ) {
                            Text("HIT DICE", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${characterViewModel.remainingHitDice} (d${characterViewModel.hitDieType})",
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }

                        // Increment Hit Dice
                        Text(
                            text = "+",
                            fontSize = 28.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clickable { characterViewModel.incrementHitDice() }
                                .padding(12.dp)
                        )
                    }
                }



                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(2.dp, borderColor), shape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DeathSavesSection(
                        successes = successes,
                        failures = failures,
                        onClick = {
                            val roll = diceRollViewModel.rollDeathSave()
                            characterViewModel.applyDeathSaveResult(roll, chatViewModel)
                            onNavigateToChat()
                        },
                        onReset = {
                            characterViewModel.resetDeathSaves()
                        }
                    )
                }
            }

            // Rest Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    characterViewModel.shortRest()
                    resourceViewModel.shortRest()
                }) {
                    Text("Short Rest")
                }
                Button(onClick = {
                    characterViewModel.longRest()
                    resourceViewModel.longRest()
                    (1..9).forEach { lvl -> spellViewModel.resetSlots(lvl) }
                }) {
                    Text("Long Rest")
                }
            }

        }
    }

    when (showEditDialogFor) {
        "CurrentHP" -> EditDialog(
            fieldName = "Current HP",
            initialValue = characterViewModel.currentHP,
            valueRange = 0..characterViewModel.maxHP,
            onDismiss = { showEditDialogFor = null },
            onApply = {
                characterViewModel.currentHP = it
                showEditDialogFor = null
            }
        )
        "MaxHP" -> EditDialog(
            fieldName = "Max HP",
            initialValue = characterViewModel.maxHP,
            valueRange = 0..999,
            onDismiss = { showEditDialogFor = null },
            onApply = {
                characterViewModel.updateMaxHP(it)
                showEditDialogFor = null
            }
        )
        "AC" -> EditDialog(
            fieldName = "Armor Class",
            initialValue = characterViewModel.armorClass,
            valueRange = 0..50,
            onDismiss = { showEditDialogFor = null },
            onApply = {
                characterViewModel.armorClass = it
                showEditDialogFor = null
            }
        )
        "Speed" -> EditDialog(
            fieldName = "Speed",
            initialValue = characterViewModel.speed,
            valueRange = 0..120,
            onDismiss = { showEditDialogFor = null },
            onApply = {
                characterViewModel.speed = it
                showEditDialogFor = null
            }
        )
        "TempHP" -> EditDialog(
            fieldName = "Temp HP",
            initialValue = characterViewModel.tempHP,
            valueRange = 0..999,
            onDismiss = { showEditDialogFor = null },
            onApply = {
                characterViewModel.tempHP = it
                showEditDialogFor = null
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    shape: RoundedCornerShape,
    borderColor: Color,
    cardColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        shape = shape,
        modifier = modifier
            .height(80.dp)
            .border(BorderStroke(2.dp, borderColor), shape),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title.uppercase(), fontSize = 12.sp, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, color = textColor)
        }
    }
}

@Composable
fun DeathSavesSection(
    successes: Int,
    failures: Int,
    onClick: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Text(
                "DEATH SAVES",
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                "↺",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onReset() }
                    .padding(end = 4.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✔", fontSize = 12.sp, color = Color.Green)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        SaveCircle(filled = index < successes, filledColor = Color.Green)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✘", fontSize = 12.sp, color = Color.Red)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        SaveCircle(filled = index < failures, filledColor = Color.Red)
                    }
                }
            }
        }
    }
}
@Composable
fun SaveCircle(filled: Boolean, filledColor: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(
                color = if (filled) filledColor else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 2.dp,
                color = Color.White,
                shape = RoundedCornerShape(50)
            )
    )
}
