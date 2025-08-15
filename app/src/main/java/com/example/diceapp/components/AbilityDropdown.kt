package com.example.diceapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.diceapp.viewModels.Ability
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AbilityDropdown(
    options: List<Ability>,
    selected: Ability?,
    onSelectedChange: (Ability) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label, color = Color.White)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(
                text = selected?.let { "${it.name} (${if (it.modifier >= 0) "+" else ""}${it.modifier})" } ?: "Select Ability",
                color = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Black)
        ) {
            options.forEach { ability ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${ability.name} (${if (ability.modifier >= 0) "+" else ""}${ability.modifier})",
                            color = Color.White
                        )
                    },
                    onClick = {
                        onSelectedChange(ability)
                        expanded = false
                    }
                )
            }
        }
    }
}
