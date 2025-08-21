package com.example.diceapp.screens

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
import com.example.diceapp.viewModels.Race

@Composable
fun CreateCharacterScreen(
    navController: NavController,
    viewModel: CreateCharacterViewModel
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var charClass by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRace by remember { mutableStateOf<Race?>(null) }

    // Characters automatisch laden
    LaunchedEffect(Unit) {
        viewModel.loadCharacters(context)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Create Character", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Class
            OutlinedTextField(
                value = charClass,
                onValueChange = { charClass = it },
                label = { Text("Class") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Level (nur Zahlen)
            OutlinedTextField(
                value = level,
                onValueChange = { level = it.filter { c -> c.isDigit() } },
                label = { Text("Level") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Race Selection
            RaceSelection(
                races = viewModel.races,
                selectedRace = selectedRace,
                onRaceSelected = { selectedRace = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Create Button
            Button(
                onClick = {
                    if (name.isBlank() || charClass.isBlank() || level.isBlank() || selectedRace == null) {
                        errorMessage = "Please fill all fields"
                    } else {
                        viewModel.createCharacter(
                            context = context,
                            name = name,
                            charClass = charClass,
                            level = level.toInt(),
                            raceName = selectedRace?.name,
                            raceDescription = selectedRace?.description,
                            onSuccess = {
                                // Formular zurücksetzen
                                name = ""
                                charClass = ""
                                level = ""
                                selectedRace = null
                                errorMessage = null
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }

            // Zurück Button
            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Zurück")
            }

            // Fehlermeldung
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Existing Characters
            Text("Existing Characters", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else if (viewModel.errorMessage != null) {
                Text(viewModel.errorMessage ?: "Error")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.characters) { character ->
                        CharacterCard(character = character,
                            onDelete = { id -> viewModel.deleteCharacter(context, id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceSelection(
    races: List<Race>,
    selectedRace: Race?,
    onRaceSelected: (Race) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRace?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Race") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            races.forEach { race ->
                DropdownMenuItem(
                    text = { Text(race.name) },
                    onClick = {
                        onRaceSelected(race)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CharacterCard(character: Character, onDelete: (Int) -> Unit) {

    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(character.name, style = MaterialTheme.typography.titleMedium)
            Text("Class: ${character.charClass}")
            Text("Level: ${character.level}")
            Text("Race: ${character.raceName ?: "Unknown"}")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${character.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(character.id)
                        showDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("No")
                }
            }
        )
    }
}
