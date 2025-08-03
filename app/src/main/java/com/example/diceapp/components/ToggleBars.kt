package com.example.diceapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke

@Composable
fun MessageModeToggle(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val options = listOf("To DM", "Public")
        options.forEach { label ->
            val isSelected = selected == label
            Button(
                onClick = { onSelect(label) },
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
fun RollModeToggle(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val options = listOf("Advantage", "Normal", "Disadvantage")
        options.forEach { label ->
            val isSelected = selected == label
            Button(
                onClick = { onSelect(label) },
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

