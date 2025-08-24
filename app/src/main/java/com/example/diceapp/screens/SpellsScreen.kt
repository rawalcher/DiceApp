package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.components.EditDialog
import com.example.diceapp.models.LocalCampaignSelection
import com.example.diceapp.models.MessageType
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.viewModels.SpellViewModel
import com.example.diceapp.viewModels.Spell
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun SpellsScreen(
    navController: NavController,
    spellViewModel: SpellViewModel,
    chatViewModel: ChatViewModel,
    diceRollViewModel: DiceRollViewModel,
    spellSaveDC: Int? = null
) {
    val spells = spellViewModel.spells
    val slots = spellViewModel.spellSlots
    val currentCampaignId = LocalCampaignSelection.current.id
    var isRemoveMode by remember { mutableStateOf(false) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleAction("−", Color.Red) { isRemoveMode = !isRemoveMode }
                    Spacer(Modifier.width(12.dp))
                    if (spellSaveDC != null) {
                        SpellDcCard(dc = spellSaveDC, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    CircleAction("+", Color(0xFF4CAF50)) { navController.navigate("add_spell") }
                }
            }

            itemsIndexed(slots, key = { _, s -> "level_${s.level}" }) { _, slot ->
                SpellLevelRow(
                    level = slot.level,
                    current = slot.current,
                    max = slot.max,
                    onIncrement = { spellViewModel.incrementSlot(slot.level) },
                    onDecrement = { spellViewModel.decrementSlot(slot.level) },
                    onSetMax = { spellViewModel.setMaxSlots(slot.level, it) }
                )

                val levelSpells = spells.filter { it.level == slot.level }
                levelSpells.forEach { spell ->
                    SpellRow(
                        spell = spell,
                        isPrepared = spell.prepared,
                        onPreparedChange = { prepared ->
                            val idx = spells.indexOf(spell)
                            if (idx >= 0) spellViewModel.setPrepared(idx, prepared)
                        },
                        onRollToHit = {
                            val bonus = spell.attackBonus ?: return@SpellRow
                            val encoded = URLEncoder.encode(spell.name, StandardCharsets.UTF_8.toString())
                            navController.navigate("dice_roll/$encoded/$bonus/0/Spell")
                        },
                        onRollDamage = {
                            if (spell.damageDice != null && spell.damageType != null) {
                                val msg = diceRollViewModel.rollDamage(
                                    label = spell.name,
                                    damageDice = spell.damageDice,
                                    damageModifier = spell.damageModifier,
                                    damageType = spell.damageType
                                )
                                chatViewModel.addMessage(msg, MessageType.ROLL, currentCampaignId)
                                navController.navigate("chat")
                            }
                        },
                        onCastSave = {
                            val dc = spell.saveDC
                            val ability = spell.saveAbility
                            if (dc != null) {
                                val header = "**${spell.name}** — target makes a ${ability ?: "—"} save (DC $dc)."
                                chatViewModel.addMessage(header, MessageType.ROLL, currentCampaignId)
                            }
                            if (spell.damageDice != null && spell.damageType != null) {
                                val dmgMsg = diceRollViewModel.rollDamage(
                                    label = spell.name,
                                    damageDice = spell.damageDice,
                                    damageModifier = spell.damageModifier,
                                    damageType = spell.damageType
                                )
                                chatViewModel.addMessage(dmgMsg, MessageType.ROLL, currentCampaignId)
                            }
                            chatViewModel.addMessage(buildSpellDescription(spell), MessageType.ROLL, currentCampaignId)
                            navController.navigate("chat")
                        },
                        onPostDescription = {
                            chatViewModel.addMessage(buildSpellDescription(spell), MessageType.ROLL, currentCampaignId)
                            navController.navigate("chat")
                        },
                        showRemoveIcon = isRemoveMode,
                        onRemoveClick = {
                            val absoluteIndex = spells.indexOf(spell)
                            if (absoluteIndex >= 0) spellViewModel.removeSpell(absoluteIndex)
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CircleAction(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
            .background(bg, shape = RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 20.sp)
    }
}

@Composable
private fun SpellDcCard(dc: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(56.dp)
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Spell DC", color = Color.LightGray, fontSize = 12.sp)
            Text(dc.toString(), color = Color.White, fontSize = 22.sp)
        }
    }
}

@Composable
private fun SpellLevelRow(
    level: Int,
    current: Int,
    max: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetMax: (Int) -> Unit
) {
    var showMaxDialog by remember { mutableStateOf(false) }
    val isCantrip = level == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LevelCircle(level)

            if (!isCantrip) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SmallCircle("−") { onDecrement() }
                    Text(
                        text = "$current / $max",
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showMaxDialog = true }
                    )
                    SmallCircle("+") { onIncrement() }
                }
            }
        }
    }

    if (showMaxDialog) {
        EditDialog(
            fieldName = "Level $level Max Slots",
            initialValue = max,
            valueRange = 0..20,
            onDismiss = { showMaxDialog = false },
            onApply = {
                onSetMax(it)
                showMaxDialog = false
            }
        )
    }
}

@Composable
private fun LevelCircle(level: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(BorderStroke(1.dp, Color.White), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(level.toString(), color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun SmallCircle(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clickable { onClick() }
            .background(Color.DarkGray, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun SpellRow(
    spell: Spell,
    isPrepared: Boolean,
    onPreparedChange: (Boolean) -> Unit,
    onRollToHit: () -> Unit,
    onRollDamage: () -> Unit,
    onCastSave: () -> Unit,
    onPostDescription: () -> Unit,
    showRemoveIcon: Boolean = false,
    onRemoveClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {

            // Top row: Prepared radio, Name, Flags, (i)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isPrepared,
                    onClick = { onPreparedChange(!isPrepared) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color.White,
                        unselectedColor = Color.White
                    )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = spell.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )

                // Flags: C R V S M
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (spell.concentration) FlagLetter("C")
                    if (spell.ritual) FlagLetter("R")

                    val comps = (spell.components ?: "").uppercase()
                    if ('V' in comps) FlagLetter("V")
                    if ('S' in comps) FlagLetter("S")
                    if ('M' in comps) FlagLetter("M")
                }

                Spacer(Modifier.width(8.dp))

                // Info toggle (circled i)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(BorderStroke(1.dp, Color.White), shape = CircleShape)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Text("i", color = Color.White, fontSize = 14.sp)
                }

                if (showRemoveIcon) {
                    Spacer(Modifier.width(8.dp))
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
            }

            // Expanded details
            if (expanded) {
                Spacer(Modifier.height(8.dp))

                // Type line
                Text(
                    text = spell.typeLabel(),
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                // Values line by type + dmg if present
                val valuesLine = when {
                    spell.attackBonus != null -> {
                        val atk = if (spell.attackBonus >= 0) "+${spell.attackBonus}" else spell.attackBonus.toString()
                        val dmg = spell.damageSummaryOrDash()
                        "Atk: $atk • $dmg"
                    }
                    spell.saveDC != null -> {
                        val savePart = "${spell.saveAbility ?: "—"} DC ${spell.saveDC}"
                        val dmg = spell.damageSummaryOrDash()
                        "Save: $savePart • $dmg"
                    }
                    else -> {
                        spell.damageSummaryOrDash()
                    }
                }
                Text(valuesLine, color = Color.White, fontSize = 14.sp)

                Spacer(Modifier.height(8.dp))

                // Buttons row based on type
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        spell.attackBonus != null -> {
                            OutlinedButton(onClick = onRollToHit) {
                                Text("Roll to hit")
                            }
                            OutlinedButton (
                                onClick = onRollDamage,
                                enabled = spell.damageDice != null && spell.damageType != null,
                            ) { Text("Damage") }
                        }
                        spell.saveDC != null -> {
                            OutlinedButton(onClick = onCastSave) {
                                Text("Cast")
                            }
                        }
                    }
                    OutlinedButton(onClick = onPostDescription) {
                        Text("Description")
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun FlagLetter(letter: String) {
    Text(letter, color = Color.White, fontSize = 12.sp)
}
private fun Spell.typeLabel(): String = when {
    attackBonus != null -> "Attack"
    saveDC != null -> "Save"
    else -> "Utility"
}
private fun Spell.damageSummaryOrDash(): String {
    return if (damageDice != null && damageType != null) {
        val mod = if (damageModifier >= 0) "+$damageModifier" else "$damageModifier"
        "Dmg: $damageDice $mod $damageType"
    } else "—"
}
private fun buildSpellDescription(spell: Spell): String {
    val flags = buildList {
        if (spell.concentration) add("Concentration")
        if (spell.ritual) add("Ritual")
    }.joinToString(", ")
    val components = spell.components ?: "—"
    val type = spell.typeLabel()
    val header = "**${spell.name}** (Lv ${spell.level}) — $type"
    val meta = "Casting Time: ${spell.castingTime} • Range: ${spell.range} • Components: $components" +
            if (flags.isNotEmpty()) " • $flags" else ""
    val dmg = if (spell.damageDice != null && spell.damageType != null)
        "\nDamage: ${spell.damageDice} ${if (spell.damageModifier >= 0) "+${spell.damageModifier}" else spell.damageModifier} ${spell.damageType}"
    else ""
    val saveOrAtk = when {
        spell.attackBonus != null -> "\nAttack Bonus: ${if (spell.attackBonus >= 0) "+${spell.attackBonus}" else spell.attackBonus}"
        spell.saveDC != null -> "\nSave: ${spell.saveAbility ?: "—"} DC ${spell.saveDC}"
        else -> ""
    }
    val body = spell.description.ifBlank { "No description." }
    return "$header\n$meta$saveOrAtk$dmg\n\n$body"
}
