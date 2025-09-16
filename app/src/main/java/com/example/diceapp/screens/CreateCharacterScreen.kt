package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ManageCharacterViewModel

/** Character creation form: local state + ability drafts; save via ManageCharacterViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    navController: NavController,
    createVM: ManageCharacterViewModel
) {
    val context = LocalContext.current
    val characterVM: CharacterViewModel = viewModel()
    //form state
    var name by rememberSaveable { mutableStateOf("") }
    var charClass by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var race by rememberSaveable { mutableStateOf("") }
    var raceDescription by rememberSaveable { mutableStateOf("") }
    var classDescription by rememberSaveable { mutableStateOf("") }
    var appearanceDescription by rememberSaveable { mutableStateOf("") }
    var backstory by rememberSaveable { mutableStateOf("") }
    var errorMessageLocal by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Character") }
            )
        }
    ) { padding ->
        // content
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // — Base fields
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = charClass, onValueChange = { charClass = it },
                    label = { Text("Class") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it.filter { c -> c.isDigit() } },
                    label = { Text("Level") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = race, onValueChange = { race = it },
                    label = { Text("Race") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = raceDescription, onValueChange = { raceDescription = it },
                    label = { Text("Race Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = classDescription, onValueChange = { classDescription = it },
                    label = { Text("Class Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    singleLine = false
                )
                OutlinedTextField(
                    value = appearanceDescription, onValueChange = { appearanceDescription = it },
                    label = { Text("Appearance (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    singleLine = false
                )
                OutlinedTextField(
                    value = backstory, onValueChange = { backstory = it },
                    label = { Text("Backstory (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    singleLine = false
                )
                // — Abilities (drafts from CharacterViewModel)
                Text("Abilities", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = characterVM.draftStr.toString(),
                        onValueChange = { v ->
                            characterVM.draftStr = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftStr
                        },
                        label = { Text("STR") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = characterVM.draftDex.toString(),
                        onValueChange = { v ->
                            characterVM.draftDex = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftDex
                        },
                        label = { Text("DEX") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = characterVM.draftCon.toString(),
                        onValueChange = { v ->
                            characterVM.draftCon = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftCon
                        },
                        label = { Text("CON") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = characterVM.draftInt.toString(),
                        onValueChange = { v ->
                            characterVM.draftInt = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftInt
                        },
                        label = { Text("INT") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = characterVM.draftWis.toString(),
                        onValueChange = { v ->
                            characterVM.draftWis = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftWis
                        },
                        label = { Text("WIS") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = characterVM.draftCha.toString(),
                        onValueChange = { v ->
                            characterVM.draftCha = v.filter(Char::isDigit).toIntOrNull()
                                ?: characterVM.draftCha
                        },
                        label = { Text("CHA") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // — Actions
            item {
                Button(
                    onClick = {
                        val requiredMissing =
                            name.isBlank() || charClass.isBlank() || level.isBlank() || race.isBlank()
                        if (requiredMissing) {
                            errorMessageLocal = "Please fill all required fields"
                        } else {
                            createVM.createCharacter(
                                context = context,
                                source = characterVM,
                                name = name,
                                charClass = charClass,
                                level = level.toInt(),
                                raceName = race,
                                raceDescription = raceDescription.ifBlank { null },
                                classDescription = classDescription.ifBlank { null },
                                appearanceDescription = appearanceDescription.ifBlank { null },
                                backstory = backstory.ifBlank { null }
                            ) {
                                name = ""; charClass = ""; level = ""; race = ""
                                raceDescription = ""; classDescription = ""
                                appearanceDescription = ""; backstory = ""
                                errorMessageLocal = null
                                characterVM.resetDraftAbilities()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create") }
            }

            item {
                Button(
                    onClick = { navController.navigate("characters") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("View Characters") }
            }

            item {
                errorMessageLocal?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                createVM.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
