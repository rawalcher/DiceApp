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
import com.example.diceapp.components.EditDialog
import com.example.diceapp.viewModels.ResourceViewModel
import com.example.diceapp.viewModels.ResetOn

@Composable
fun AdditionalResourcesScreen(
    navController: NavController,
    resourceViewModel: ResourceViewModel
) {
    var isRemoveMode by remember { mutableStateOf(false) }
    val resources = resourceViewModel.resources

    Scaffold(containerColor = Color.Black) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top action row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleAction("−", Color.Red) { isRemoveMode = !isRemoveMode }
                    Spacer(Modifier.weight(1f))
                    CircleAction("+", Color(0xFF4CAF50)) { navController.navigate("add_resource") }
                }
            }

            // Resource cards
            itemsIndexed(resources, key = { idx, _ -> "res_$idx" }) { index, res ->
                ResourceCardRow(
                    name = res.name,
                    current = res.current,
                    max = res.max,
                    resetOn = res.resetOn,
                    showRemoveIcon = isRemoveMode,
                    onEditCurrent = { newValue -> resourceViewModel.setCurrent(index, newValue) },
                    onRemove = { resourceViewModel.removeResource(index) }
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
private fun ResourceCardRow(
    name: String,
    current: Int,
    max: Int,
    resetOn: ResetOn,
    showRemoveIcon: Boolean,
    onEditCurrent: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

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
                ) {
                    Text("−", color = Color.White)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                ResetTag(resetOn)
            }

            Text(
                text = "$current / $max",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { showDialog = true }
            )
        }
    }

    if (showDialog) {
        EditDialog(
            fieldName = "$name (Current)",
            initialValue = current,
            valueRange = 0..max.coerceAtLeast(0),
            onDismiss = { showDialog = false },
            onApply = {
                onEditCurrent(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ResetTag(resetOn: ResetOn) {
    val label = when (resetOn) {
        ResetOn.SHORT_REST -> "Short Rest"
        ResetOn.LONG_REST  -> "Long Rest"
        ResetOn.NONE       -> "No Reset"
    }
    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, Color.White), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
