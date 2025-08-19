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
import com.example.diceapp.viewModels.ResetOn
import com.example.diceapp.viewModels.ResourceViewModel

@Composable
fun AddResourceScreen(
    navController: NavController,
    resourceViewModel: ResourceViewModel
) {
    var name by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }
    var resetOn by remember { mutableStateOf(ResetOn.SHORT_REST) }

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Add Resource",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true
            )

            OutlinedTextField(
                value = maxText,
                onValueChange = { maxText = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Max Value", color = Color.White) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true
            )

            Text("Resets on", color = Color.White)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val Chip: @Composable (Boolean, String, () -> Unit) -> Unit = { selected, text, onClick ->
                    if (selected) Button(onClick = onClick) { Text(text) }
                    else OutlinedButton(onClick = onClick) { Text(text) }
                }
                Chip(resetOn == ResetOn.SHORT_REST, "Short Rest") { resetOn = ResetOn.SHORT_REST }
                Chip(resetOn == ResetOn.LONG_REST,  "Long Rest")  { resetOn = ResetOn.LONG_REST }
                Chip(resetOn == ResetOn.NONE,       "No Reset")   { resetOn = ResetOn.NONE }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val max = maxText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        if (name.isNotBlank()) {
                            resourceViewModel.addResource(name.trim(), max, resetOn)
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
