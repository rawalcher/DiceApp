package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.AuthViewModel

@Composable
fun MainMenuScreen(navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current

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
            Button(onClick = { navController.navigate("create_character") }) {
                Text("Create Character")
            }
            Button(onClick = { navController.navigate("combat_stats") }) {
                Text("Combat Stats")
            }
            Button(onClick = { navController.navigate("chat") }) {
                Text("Chat")
            }
            Button(onClick = { navController.navigate("attack") }) {
                Text("Attacks")
            }
            Button(onClick = { navController.navigate("spells") }) {
                Text("Spells")
            }
            Button(onClick = { navController.navigate("resources") }) {
                Text("Additional Resources")
            }
            Button(onClick = { navController.navigate("modifiers") }) {
                Text("Additional Modifiers")
            }
            Button(onClick = { navController.navigate("dm_level_up") }) {
                Text("DM Level Up")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    authViewModel.logout(context)
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        }
    }
}