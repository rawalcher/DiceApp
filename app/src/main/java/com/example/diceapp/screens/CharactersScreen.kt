package com.example.diceapp.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.Character
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ManageCharacterViewModel

/** Character list with compact cards; tap opens full-screen detail/editor overlay. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    navController: NavController,
    createVM: ManageCharacterViewModel,
    characterVM: CharacterViewModel
) {
    val context = LocalContext.current
    var fullscreenCharacterId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showCampaignPicker by rememberSaveable { mutableStateOf(false) }

    val fullscreenCharacter = remember(createVM.characters, fullscreenCharacterId) {
        createVM.characters.firstOrNull { it.id == fullscreenCharacterId }
    }

    LaunchedEffect(Unit) { createVM.loadCharacters(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Characters") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // — List
            if (createVM.isLoading && createVM.characters.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = createVM.characters,
                        key = { ch -> ch.id }
                    ) { character ->
                        CharacterCardSmall(
                            character = character,
                            onClick = { fullscreenCharacterId = character.id },
                            onDelete = { id -> createVM.deleteCharacter(context, id) }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
            // — Overlay
            fullscreenCharacter?.let { character ->
                CharacterDetailCard(
                    character = character,
                    createVM = createVM,
                    characterVM = characterVM,
                    onClose = { fullscreenCharacterId = null },
                    showDeleteDialog = showDeleteDialog,
                    setShowDeleteDialog = { showDeleteDialog = it },
                    showCampaignPicker = showCampaignPicker,
                    setShowCampaignPicker = { showCampaignPicker = it }
                )
            }
        }
    }
}
/** Compact list card (tap → open overlay). */
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
/** Full-screen character detail/editor; handles read/edit, campaign join/leave, save/delete. */
@Composable
private fun CharacterDetailCard(
    character: Character,
    createVM: ManageCharacterViewModel,
    characterVM: CharacterViewModel,
    onClose: () -> Unit,
    showDeleteDialog: Boolean,
    setShowDeleteDialog: (Boolean) -> Unit,
    showCampaignPicker: Boolean,
    setShowCampaignPicker: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isEditing by rememberSaveable(character.id) { mutableStateOf(false) }
    // editor fields
    var eName by rememberSaveable(character.id) { mutableStateOf(character.name) }
    var eClass by rememberSaveable(character.id) { mutableStateOf(character.charClass) }
    var eLevel by rememberSaveable(character.id) { mutableStateOf(character.level.toString()) }
    var eRace by rememberSaveable(character.id) { mutableStateOf(character.raceName ?: "") }
    var eRaceDesc by rememberSaveable(character.id) { mutableStateOf(character.raceDescription ?: "") }
    var eClassDesc by rememberSaveable(character.id) { mutableStateOf(character.classDescription ?: "") }
    var eAppear by rememberSaveable(character.id) { mutableStateOf(character.appearanceDescription ?: "") }
    var eBackstory by rememberSaveable(character.id) { mutableStateOf(character.backstory ?: "") }

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
            .clickable(enabled = false) { }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Text(
                        if (isEditing) "Edit Character" else "Character Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        if (isEditing) eName else character.name,
                        style = MaterialTheme.typography.displaySmall
                    )

                    HorizontalDivider()

                    if (isEditing) {
                        OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eClass, onValueChange = { eClass = it }, label = { Text("Class") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eLevel, onValueChange = { eLevel = it.filter { ch -> ch.isDigit() } }, label = { Text("Level") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eRace, onValueChange = { eRace = it }, label = { Text("Race") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eRaceDesc, onValueChange = { eRaceDesc = it }, label = { Text("Race Description (optional)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eClassDesc, onValueChange = { eClassDesc = it }, label = { Text("Class Description (optional)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eAppear, onValueChange = { eAppear = it }, label = { Text("Appearance (optional)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = eBackstory, onValueChange = { eBackstory = it }, label = { Text("Backstory (optional)") }, modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.height(12.dp))
                        Text("Abilities", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = characterVM.draftStr.toString(),
                                onValueChange = { v -> characterVM.draftStr = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftStr },
                                label = { Text("STR") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = characterVM.draftDex.toString(),
                                onValueChange = { v -> characterVM.draftDex = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftDex },
                                label = { Text("DEX") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = characterVM.draftCon.toString(),
                                onValueChange = { v -> characterVM.draftCon = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftCon },
                                label = { Text("CON") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = characterVM.draftInt.toString(),
                                onValueChange = { v -> characterVM.draftInt = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftInt },
                                label = { Text("INT") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = characterVM.draftWis.toString(),
                                onValueChange = { v -> characterVM.draftWis = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftWis },
                                label = { Text("WIS") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = characterVM.draftCha.toString(),
                                onValueChange = { v -> characterVM.draftCha = v.filter(Char::isDigit).toIntOrNull() ?: characterVM.draftCha },
                                label = { Text("CHA") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        InfoRow("LVL", character.level.toString())
                        InfoRow("Campaign", character.campaignName ?: "—")
                        InfoRow("Class", character.charClass.ifBlank { "—" })
                        InfoRow("Race", character.raceName ?: "—")
                        InfoRow("Race Description", character.raceDescription?.ifBlank { "—" } ?: "—", multiline = true)
                        InfoRow("Class Description", character.classDescription?.ifBlank { "—" } ?: "—", multiline = true)
                        InfoRow("Appearance", character.appearanceDescription?.ifBlank { "—" } ?: "—", multiline = true)
                        InfoRow("Backstory", character.backstory?.ifBlank { "—" } ?: "—", multiline = true)

                        Spacer(Modifier.height(8.dp))
                        Text("Abilities", style = MaterialTheme.typography.titleMedium)
                        AbilityGrid(
                            str = character.strength,
                            dex = character.dexterity,
                            con = character.constitution,
                            int_ = character.intelligence,
                            wis = character.wisdom,
                            cha = character.charisma
                        )

                        HorizontalDivider()
                        // Campaign actions (join/leave/switch)
                        val isAssigned = (character.campaignId != null) || (character.campaignName != null)
                        if (!isAssigned) {
                            Button(
                                onClick = {
                                    setShowCampaignPicker(true)
                                    createVM.loadCampaigns(context)
                                },
                                colors = actionColors,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Join Campaign") }
                        } else {
                            Button(
                                onClick = { createVM.unassignCharacter(context, character.id) },
                                colors = actionColors,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Leave Campaign") }

                            Spacer(Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    setShowCampaignPicker(true)
                                    createVM.loadCampaigns(context)
                                },
                                colors = actionColors,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Choose Another Campaign") }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Enter edit mode and seed ability drafts from current character
                                characterVM.draftStr = character.strength
                                characterVM.draftStr = character.strength
                                characterVM.draftDex = character.dexterity
                                characterVM.draftCon = character.constitution
                                characterVM.draftInt = character.intelligence
                                characterVM.draftWis = character.wisdom
                                characterVM.draftCha = character.charisma
                                isEditing = true
                                resetEditorsFrom(character)
                            },
                            colors = editColors,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Edit") }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { setShowDeleteDialog(true) },
                            colors = dangerColors,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Delete Character") }
                    }
                }
                // Card actions: Save/Cancel in edit; Close in read-only
                if (isEditing) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                                ) { onClose() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !createVM.isLoading
                        ) { Text(if (createVM.isLoading) "Saving..." else "Save") }

                        OutlinedButton(
                            onClick = {
                                isEditing = false
                                resetEditorsFrom(character)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !createVM.isLoading
                        ) { Text("Cancel") }

                        createVM.errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { setShowDeleteDialog(false) },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${character.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    setShowDeleteDialog(false)
                    onClose()
                    createVM.deleteCharacter(context, character.id) { }
                }) { Text("Yes") }

            },
            dismissButton = {
                TextButton(onClick = { setShowDeleteDialog(false) }) { Text("No") }
            }
        )
    }

    if (showCampaignPicker) {
        CampaignPickerDialog(
            isLoading = createVM.isLoading || createVM.isAssigning,
            error = createVM.errorMessage,
            campaigns = createVM.campaigns,
            onClose = { setShowCampaignPicker(false) },
            onSelect = { selected ->
                createVM.assignCharacterToCampaign(context, character.id, selected) {
                    setShowCampaignPicker(false)
                }
            }

        )
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
private fun AbilityGrid(
    str: Int?, dex: Int?, con: Int?, int_: Int?, wis: Int?, cha: Int?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityBox("STR", str, modifier = Modifier.weight(1f))
            AbilityBox("DEX", dex, modifier = Modifier.weight(1f))
            AbilityBox("CON", con, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityBox("INT", int_, modifier = Modifier.weight(1f))
            AbilityBox("WIS", wis, modifier = Modifier.weight(1f))
            AbilityBox("CHA", cha, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AbilityBox(label: String, value: Int?, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.heightIn(min = 64.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value?.toString() ?: "—", style = MaterialTheme.typography.titleLarge)
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
        title = { Text("Select Campaign") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isLoading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                } else if (campaigns.isEmpty()) {
                    Text("No campaigns found.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = campaigns, key = { it.id }) { camp ->
                            val isFull = camp.playerCount >= camp.maxPlayers
                            val canAssign = camp.isJoined && !isFull

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = canAssign && !isLoading) { onSelect(camp) },
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(camp.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(2.dp))
                                    Text(camp.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Players: ${camp.playerCount}/${camp.maxPlayers}", style = MaterialTheme.typography.labelMedium)

                                    if (!camp.isJoined) {
                                        Text("You haven't joined this campaign", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    } else if (isFull) {
                                        Text("Campaign is full", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
