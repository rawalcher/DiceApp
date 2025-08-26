package com.example.diceapp.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diceapp.viewModels.Character
import com.example.diceapp.viewModels.CreateCharacterViewModel
import com.example.diceapp.viewModels.CharacterViewModel

@Composable
fun CreateCharacterScreen(
    navController: NavController,
    createVM: CreateCharacterViewModel
) {
    val context = LocalContext.current
    val characterVM: CharacterViewModel = viewModel()
    var name by rememberSaveable { mutableStateOf("") }
    var charClass by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var race by rememberSaveable { mutableStateOf("") }
    var raceDescription by rememberSaveable { mutableStateOf("") }
    var classDescription by rememberSaveable { mutableStateOf("") }
    var appearanceDescription by rememberSaveable { mutableStateOf("") }
    var backstory by rememberSaveable { mutableStateOf("") }
    var errorMessageLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var fullscreenCharacterId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCampaignPicker by rememberSaveable { mutableStateOf(false) }
    val fullscreenCharacter = remember(createVM.characters, fullscreenCharacterId) {
        createVM.characters.firstOrNull { it.id == fullscreenCharacterId }
    }

    LaunchedEffect(Unit) { createVM.loadCharacters(context) }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Create Character", style = MaterialTheme.typography.headlineMedium)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            label = { Text("Level") },
                            modifier = Modifier.fillMaxWidth()
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
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (name.isBlank() || charClass.isBlank() || level.isBlank() || race.isBlank()) {
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
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create") }
                }

                item {
                    Button(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back") }
                }

                item {
                    Column {
                        errorMessageLocal?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        createVM.errorMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                item {
                    Text("Existing Characters", style = MaterialTheme.typography.headlineSmall)
                }

                if (createVM.isLoading) {
                    item { CircularProgressIndicator() }
                } else {
                    items(createVM.characters) { character ->
                        CharacterCardSmall(
                            character = character,
                            onClick = { fullscreenCharacterId = character.id },
                            onDelete = { id -> createVM.deleteCharacter(context, id) }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }

            fullscreenCharacter?.let { character ->
                var isEditing by rememberSaveable(fullscreenCharacterId) { mutableStateOf(false) }
                val scrollState = rememberSaveable(fullscreenCharacterId, saver = ScrollState.Saver) { ScrollState(0) }

                var eName by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.name) }
                var eClass by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.charClass) }
                var eLevel by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.level.toString()) }
                var eRace by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.raceName ?: "") }
                var eRaceDesc by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.raceDescription ?: "") }
                var eClassDesc by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.classDescription ?: "") }
                var eAppear by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.appearanceDescription ?: "") }
                var eBackstory by rememberSaveable(fullscreenCharacterId) { mutableStateOf(character.backstory ?: "") }

                fun resetEditorsFrom(c: Character) {
                    eName = c.name
                    eClass = c.charClass
                    eLevel = c.level.toString()
                    eRace = c.raceName ?: ""
                    eRaceDesc = c.raceDescription ?: ""
                    eClassDesc = c.classDescription ?: ""
                    eAppear = c.appearanceDescription ?: ""
                    eBackstory = c.backstory ?: ""
                }

                val actionColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                val editColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val dangerColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clickable(enabled = false) { /* consume */ }
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (isEditing) "Edit Character" else "Character Details",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            // Content scrollable
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = true)
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    if (isEditing) eName else character.name,
                                    style = MaterialTheme.typography.displaySmall
                                )

                                HorizontalDivider()

                                if (isEditing) {
                                    OutlinedTextField(
                                        value = eName, onValueChange = { eName = it },
                                        label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eClass, onValueChange = { eClass = it },
                                        label = { Text("Class") }, modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eLevel,
                                        onValueChange = { eLevel = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Level") }, modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eRace, onValueChange = { eRace = it },
                                        label = { Text("Race") }, modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eRaceDesc, onValueChange = { eRaceDesc = it },
                                        label = { Text("Race Description (optional)") }, modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eClassDesc, onValueChange = { eClassDesc = it },
                                        label = { Text("Class Description (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eAppear, onValueChange = { eAppear = it },
                                        label = { Text("Appearance (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = eBackstory, onValueChange = { eBackstory = it },
                                        label = { Text("Backstory (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(12.dp))
                                } else {
                                    InfoRow("LVL", character.level.toString())
                                    InfoRow("Campaign", character.campaignName ?: "—")
                                    InfoRow("Class", character.charClass.ifBlank { "—" })
                                    InfoRow("Race", character.raceName ?: "—")
                                    InfoRow(
                                        "Race Description",
                                        character.raceDescription?.ifBlank { "—" } ?: "—",
                                        multiline = true
                                    )
                                    InfoRow(
                                        "Class Description",
                                        character.classDescription?.ifBlank { "—" } ?: "—",
                                        multiline = true
                                    )
                                    InfoRow(
                                        "Appearance",
                                        character.appearanceDescription?.ifBlank { "—" } ?: "—",
                                        multiline = true
                                    )
                                    InfoRow(
                                        "Backstory",
                                        character.backstory?.ifBlank { "—" } ?: "—",
                                        multiline = true
                                    )

                                    HorizontalDivider()

                                    val isAssigned = (character.campaignId != null) || (character.campaignName != null)
                                    if (!isAssigned) {
                                        Button(
                                            onClick = {
                                                showCampaignPicker = true
                                                createVM.loadCampaigns(context)
                                            },
                                            colors = actionColors,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Join Campaign") }
                                    } else {
                                        Button(
                                            onClick = {
                                                createVM.unassignCharacter(
                                                    context = context,
                                                    characterId = character.id
                                                )
                                            },
                                            colors = actionColors,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Leave Campaign") }

                                        Spacer(Modifier.height(6.dp))

                                        Button(
                                            onClick = {
                                                showCampaignPicker = true
                                                createVM.loadCampaigns(context)
                                            },
                                            colors = actionColors,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Choose Another Campaign") }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            isEditing = true
                                            resetEditorsFrom(character)
                                        },
                                        colors = editColors,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Edit") }

                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { showDeleteDialog = true },
                                        colors = dangerColors,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Delete Character") }
                                }
                            }

                            if (isEditing) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            val levelInt = eLevel.toIntOrNull()
                                            if (eName.isBlank() || eClass.isBlank() || levelInt == null) return@Button
                                            createVM.updateCharacter(
                                                context = context,
                                                characterId = character.id,
                                                source = characterVM,
                                                name = eName,
                                                charClass = eClass,
                                                level = levelInt,
                                                raceName = eRace.ifBlank { null },
                                                raceDescription = eRaceDesc.ifBlank { null },
                                                classDescription = eClassDesc.ifBlank { null },
                                                appearanceDescription = eAppear.ifBlank { null },
                                                backstory = eBackstory.ifBlank { null }
                                            ) {
                                                fullscreenCharacterId = null
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Save") }

                                    OutlinedButton(
                                        onClick = {
                                            isEditing = false
                                            resetEditorsFrom(character)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Cancel") }
                                }
                            } else {
                                TextButton(
                                    onClick = { fullscreenCharacterId = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Close") }
                            }
                        }
                    }
                }

                // Delete Dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Confirm Delete") },
                        text = { Text("Are you sure you want to delete ${character.name}?") },
                        confirmButton = {
                            TextButton(onClick = {
                                createVM.deleteCharacter(context, character.id)
                                showDeleteDialog = false
                                fullscreenCharacterId = null
                            }) { Text("Yes") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("No") }
                        }
                    )
                }

                if (showCampaignPicker) {
                    CampaignPickerDialog(
                        isLoading = createVM.isLoading || createVM.isAssigning,
                        error = createVM.errorMessage, // <-- Fehler im Dialog anzeigen
                        campaigns = createVM.campaigns,
                        onClose = { showCampaignPicker = false },
                        onSelect = { selected ->
                            val id = fullscreenCharacterId ?: return@CampaignPickerDialog
                            createVM.assignCharacterToCampaign(
                                context = context,
                                characterId = id,
                                campaignId = selected.id
                            ) {
                                showCampaignPicker = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    multiline: Boolean = false
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        if (multiline) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun CharacterCardSmall(
    character: Character,
    onClick: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            character.campaignName?.let { campaignName ->
                Text("Campaign: $campaignName", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
            Text(character.name, style = MaterialTheme.typography.titleMedium)
            Text("Class: ${character.charClass}")
            Text("Race: ${character.raceName ?: "Unknown"}")
            Text("Level: ${character.level}")
        }
    }
}

@Composable
private fun CampaignPickerDialog(
    isLoading: Boolean,
    error: String?,
    campaigns: List<com.example.diceapp.viewModels.Campaign>,
    onClose: () -> Unit,
    onSelect: (campaign: com.example.diceapp.viewModels.Campaign) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Kampagne auswählen") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isLoading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                } else if (campaigns.isEmpty()) {
                    Text("Keine Kampagnen gefunden.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(campaigns) { camp ->
                            val isFull = camp.playerCount >= camp.maxPlayers
                            val canAssign = camp.isJoined && !isFull

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = canAssign && !isLoading) { onSelect(camp) },
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        camp.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        camp.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Spieler: ${camp.playerCount}/${camp.maxPlayers}",
                                        style = MaterialTheme.typography.labelMedium
                                    )

                                    if (!camp.isJoined) {
                                        Text(
                                            "Du bist dieser Kampagne nicht beigetreten",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else if (isFull) {
                                        Text(
                                            "Kampagne ist voll",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Schließen") } }
    )
}
