package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MainMenuScreen(navController: NavController) {
    Scaffold{ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dice App", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { navController.navigate("campaigns") }) {
                Text("Campaigns")
            }
            Button(onClick = { navController.navigate("stats") }) {
                Text("Stats")
            }
            Button(onClick = { navController.navigate("saving_throws") }) {
                Text("Saving Throws")
            }
            Button(onClick = { navController.navigate("skills") }) {
                Text("Skills")
            }
            Button(onClick = { navController.navigate("combat_stats") }) {
                Text("Combat Stats")
            }
            Button(onClick = { navController.navigate("chat") }) {
                Text("Chat")
            }
            Button(onClick = { navController.navigate("login") }) {
                Text("Login")
            }
        }
    }
}