package com.example.diceapp.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.Character
import com.example.diceapp.viewModels.CreateCharacterViewModel

@Composable
fun CreateCharacterScreen(
    navController: NavController,
    viewModel: CreateCharacterViewModel
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var charClass by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var race by remember { mutableStateOf("") }
    var raceDescription by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fullscreen Character Overlay
    var fullscreenCharacter by remember { mutableStateOf<Character?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Characters automatisch laden
    LaunchedEffect(Unit) {
        viewModel.loadCharacters(context)
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                Text("Create Character", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // --- Form Fields ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = charClass,
                    onValueChange = { charClass = it },
                    label = { Text("Class") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it.filter { c -> c.isDigit() } },
                    label = { Text("Level") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = race,
                    onValueChange = { race = it },
                    label = { Text("Race") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = raceDescription,
                    onValueChange = { raceDescription = it },
                    label = { Text("Race Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Buttons ---
                Button(
                    onClick = {
                        if (name.isBlank() || charClass.isBlank() || level.isBlank() || race.isBlank()) {
                            errorMessage = "Please fill all fields"
                        } else {
                            viewModel.createCharacter(
                                context = context,
                                name = name,
                                charClass = charClass,
                                level = level.toInt(),
                                raceName = race,
                                raceDescription = raceDescription,
                                onSuccess = {
                                    name = ""
                                    charClass = ""
                                    level = ""
                                    race = ""
                                    raceDescription = ""
                                    errorMessage = null
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create")
                }

                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Zurück")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Existing Characters", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.isLoading) {
                    CircularProgressIndicator()
                } else if (viewModel.errorMessage != null) {
                    Text(viewModel.errorMessage ?: "Error")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.characters) { character ->
                            CharacterCardSmall(
                                character = character,
                                onClick = { fullscreenCharacter = character },
                                onDelete = { id -> viewModel.deleteCharacter(context, id) }
                            )
                        }
                    }
                }
            }

            // --- Fullscreen Overlay ---
            fullscreenCharacter?.let { character ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { fullscreenCharacter = null }
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(character.name, style = MaterialTheme.typography.headlineLarge)
                            Text("Class: ${character.charClass}", style = MaterialTheme.typography.titleMedium)
                            Text("Race: ${character.raceName ?: "Unknown"}", style = MaterialTheme.typography.titleMedium)
                            Text("Level: ${character.level}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Race Description: ${character.raceDescription ?: "None"}")
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete Character")
                            }
                        }
                    }
                }

                // --- Delete Dialog ---
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Confirm Delete") },
                        text = { Text("Are you sure you want to delete ${character.name}?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteCharacter(context, character.id)
                                showDialog = false
                                fullscreenCharacter = null
                            }) { Text("Yes") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) { Text("No") }
                        }
                    )
                }
            }
        }
    }
}

// Kleine Card für die Liste
@Composable
fun CharacterCardSmall(character: Character, onClick: () -> Unit, onDelete: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(character.name, style = MaterialTheme.typography.titleMedium)
            Text("Class: ${character.charClass}")
            Text("Race: ${character.raceName ?: "Unknown"}")
            Text("Level: ${character.level}")
        }
    }
}
