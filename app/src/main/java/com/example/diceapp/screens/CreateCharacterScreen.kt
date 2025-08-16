package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.CharacterData
import com.example.diceapp.viewModels.Race

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    navController: NavController,
    characterViewModel: CharacterViewModel,
    onCharacterCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var charClass by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRace by remember { mutableStateOf<Race?>(null) }

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Race Dropdown
            RaceDropdown(
                characterViewModel = characterViewModel,
                selectedRace = selectedRace,
                onRaceSelected = { selectedRace = it }
            )

            selectedRace?.let { race ->
                Spacer(modifier = Modifier.height(8.dp))
                race.description.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || charClass.isBlank() || level.isBlank() || selectedRace == null) {
                        errorMessage = "Please fill all fields"
                    } else {
                        // Charakter erstellen inkl. Rasse
                        characterViewModel.addCharacter(
                            name = name,
                            charClass = charClass,
                            level = level.toInt(),
                            race = selectedRace
                        )
                        // Felder zurücksetzen, aber auf derselben Seite bleiben
                        name = ""
                        charClass = ""
                        level = ""
                        selectedRace = null
                        errorMessage = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }

            Button(
                onClick = { navController.navigateUp() }, // geht eine Ebene zurück
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

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(characterViewModel.characters) { character ->
                    CharacterCard(character = character)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDropdown(
    characterViewModel: CharacterViewModel,
    selectedRace: Race?,
    onRaceSelected: (Race) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isAddingRace by remember { mutableStateOf(false) }
    var newRaceName by remember { mutableStateOf("") }
    var newRaceDescription by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = selectedRace?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Race") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                characterViewModel.races.forEach { race ->
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

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { isAddingRace = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add Race")
        }
    }

    if (isAddingRace) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            OutlinedTextField(
                value = newRaceName,
                onValueChange = { newRaceName = it },
                label = { Text("Neue Rasse") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = newRaceDescription,
                onValueChange = { newRaceDescription = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                if (newRaceName.isNotBlank()) {
                    val race = Race(newRaceName, newRaceDescription)
                    characterViewModel.addRace(race.name, race.description)
                    onRaceSelected(race)
                    newRaceName = ""
                    newRaceDescription = ""
                    isAddingRace = false
                }
            }) {
                Text("OK")
            }
        }
    }
}

@Composable
fun CharacterCard(character: CharacterData) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Name: ${character.name}", fontSize = 16.sp)
            Text("Klasse: ${character.charClass}", fontSize = 16.sp)
            Text("Rasse: ${character.race?.name ?: "-"}", fontSize = 16.sp)
            character.race?.description?.takeIf { it.isNotBlank() }?.let {
                Text("Rassenbeschreibung: $it", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Level: ${character.level}", fontSize = 16.sp)
        }
    }
}