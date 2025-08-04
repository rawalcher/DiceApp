package com.example.diceapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.diceapp.ViewModels.CharacterViewModel
import com.example.diceapp.ViewModels.ChatViewModel
import com.example.diceapp.screens.ChatScreen
import com.example.diceapp.screens.DiceRollScreen
import com.example.diceapp.screens.MainMenuScreen
import com.example.diceapp.screens.StatsScreen
import androidx.compose.foundation.layout.padding
import com.example.diceapp.ViewModels.DiceRollViewModel




@Composable
fun AppNavHost(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel,
    diceRollViewModel: DiceRollViewModel
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // Apply scaffold padding here
        ) {
            NavHost(
                navController = navController,
                startDestination = "menu",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("menu") {
                    MainMenuScreen(navController = navController)
                }
                composable("stats") {
                    StatsScreen(
                        chatViewModel = chatViewModel,
                        characterViewModel = characterViewModel,
                        navController = navController
                    )
                }
                composable("chat") {
                    ChatScreen(chatViewModel = chatViewModel)
                }
                composable(
                    route = "dice_roll/{label}/{modifier}/{proficiencyBonus}",
                    arguments = listOf(
                        navArgument("label") { type = NavType.StringType },
                        navArgument("modifier") { type = NavType.IntType },
                        navArgument("proficiencyBonus") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val label = backStackEntry.arguments?.getString("label") ?: "Roll"
                    val modifier = backStackEntry.arguments?.getInt("modifier") ?: 0
                    val proficiencyBonus = backStackEntry.arguments?.getInt("proficiencyBonus") ?: 0

                    DiceRollScreen(
                        rollLabel = label,
                        modifier = modifier,
                        proficiencyBonus = proficiencyBonus,
                        navController = navController,
                        chatViewModel = chatViewModel,
                        diceRollViewModel = diceRollViewModel
                    )
                }
            }
        }
    }
}
