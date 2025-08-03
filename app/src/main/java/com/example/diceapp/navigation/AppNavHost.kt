package com.example.diceapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.diceapp.screens.ChatScreen
import com.example.diceapp.ViewModels.ChatViewModel
import com.example.diceapp.ViewModels.CharacterViewModel
import com.example.diceapp.screens.StatsScreen
import com.example.diceapp.screens.MainMenuScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel
) {
    Scaffold { padding -> padding
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "menu",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("menu") {
                    MainMenuScreen(navController = navController)
                }
                composable("roll_stats") {
                    StatsScreen(
                        chatViewModel = chatViewModel,
                        characterViewModel = characterViewModel
                    )
                }
                composable("chat") {
                    ChatScreen(chatViewModel = chatViewModel)
                }
            }
        }
    }
}
