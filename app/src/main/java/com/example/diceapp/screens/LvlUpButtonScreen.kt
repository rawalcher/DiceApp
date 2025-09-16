package com.example.diceapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.CampaignCharacter
import com.example.diceapp.viewModels.LvlUpButtonViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LvlUpButtonScreen(
    navController: NavController,
    viewModel: LvlUpButtonViewModel
) {
    val context = LocalContext.current
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val error = viewModel.errorMessage
    val owned = viewModel.ownedCampaigns
    val selected = viewModel.selectedCampaign
    val characters = viewModel.characters

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // initial load
    LaunchedEffect(Unit) { viewModel.loadCampaigns(context) }

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DM: Level Up") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campaign selector (owned only)
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selected?.name ?: "Select campaign",
                    onValueChange = {},
                    label = { Text("Your campaigns") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    owned.forEach { camp ->
                        DropdownMenuItem(
                            text = { Text(camp.name) },
                            onClick = {
                                menuExpanded = false
                                viewModel.onSelectCampaign(context, camp)
                            }
                        )
                    }
                }
            }

            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            // Header + Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.let { "Players in: ${it.name}" } ?: "No campaign selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val camp = selected ?: return@Button
                        viewModel.levelUpCampaign(
                            context = context,
                            campaignId = camp.id,
                            onResult = { updated ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (updated > 0)
                                            "Level up granted: $updated character(s)."
                                        else
                                            "There are no characters in this campaign to level up."
                                    )
                                }
                            },
                            onError = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    },
                    enabled = selected != null && !isLoading
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Level Up")
                }
            }

            ElevatedCard(Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (selected == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a campaign above.")
                    }
                } else if (characters.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No players/characters in this campaign.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(characters) { c ->
                            CharacterRow(c)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterRow(c: CampaignCharacter) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text("Class: ${c.charClass}", style = MaterialTheme.typography.bodySmall)
                c.raceName?.let { Text("Race: $it", style = MaterialTheme.typography.bodySmall) }
            }
            AssistChip(
                onClick = {},
                label = { Text("LVL ${c.level}") }
            )
        }
    }
}
