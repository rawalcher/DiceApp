package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.AppliesTo
import com.example.diceapp.viewModels.ModifierViewModel
import com.example.diceapp.viewModels.RollModifier

@Composable
fun AdditionalModifiersScreen(
    navController: NavController,
    modifierViewModel: ModifierViewModel
) {
    var isRemoveMode by remember { mutableStateOf(false) }
    val items = modifierViewModel.modifiers

    Scaffold(containerColor = Color.Black) { padding ->
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
                    Spacer(Modifier.weight(1f))
                    CircleAction("+", Color(0xFF4CAF50)) { navController.navigate("add_modifier") }
                }
            }

            itemsIndexed(items, key = { idx, _ -> "mod_$idx" }) { index, mod ->
                ModifierCardRow(
                    modifier = mod,
                    showRemoveIcon = isRemoveMode,
                    onToggle = { modifierViewModel.toggleEnabled(index) },
                    onRemove = { modifierViewModel.removeModifier(index) }
                )
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
        Text(text = label, color = Color.White)
    }
}

@Composable
private fun ModifierCardRow(
    modifier: RollModifier,
    showRemoveIcon: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showRemoveIcon) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Red, shape = RoundedCornerShape(50))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) { Text("−", color = Color.White) }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = modifier.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(modifier.appliesTo)
                    if (modifier.flatBonus != 0) Pill(text = if (modifier.flatBonus > 0) "+${modifier.flatBonus}" else "${modifier.flatBonus}")
                    modifier.extraDice?.takeIf { it.isNotBlank() }?.let { Pill(text = it) }
                }
            }

            RadioButton(
                selected = modifier.enabled,
                onClick = onToggle,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = Color.Gray
                )
            )
        }
    }
}


@Composable
private fun Pill(appliesTo: AppliesTo) {
    val label = when (appliesTo) {
        AppliesTo.ATTACK -> "Attack"
        AppliesTo.SAVE -> "Save"
        AppliesTo.CHECK -> "Check"
        AppliesTo.ALL -> "All"
    }
    Pill(label)
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
