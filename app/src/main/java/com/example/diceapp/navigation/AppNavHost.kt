package com.example.diceapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.diceapp.ViewModels.DiceRollViewModel
import com.example.diceapp.screens.ChatScreen
import com.example.diceapp.screens.DiceRollScreen
import com.example.diceapp.screens.MainMenuScreen
import com.example.diceapp.screens.StatsScreen
import com.example.diceapp.screens.SavingThrowsScreen
import com.example.diceapp.screens.SkillsScreen
import com.example.diceapp.screens.CombatStatsScreen


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
                .padding(padding)
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
                        characterViewModel = characterViewModel,
                        navController = navController
                    )
                }
                composable("chat") {
                    ChatScreen(chatViewModel = chatViewModel)
                }
                composable("saving_throws") {
                    SavingThrowsScreen(
                        navController = navController,
                        characterViewModel = characterViewModel
                    )
                }
                composable("skills") {
                    SkillsScreen(
                        navController = navController,
                        characterViewModel = characterViewModel
                    )
                }
                composable("combat_stats") {
                    CombatStatsScreen(
                        characterViewModel = characterViewModel,
                        chatViewModel = chatViewModel
                    )
                }


                composable(
                    route = "dice_roll/{label}/{modifier}/{proficiencyBonus}/{type}",
                    arguments = listOf(
                        navArgument("label") { type = NavType.StringType },
                        navArgument("modifier") { type = NavType.IntType },
                        navArgument("proficiencyBonus") {
                            type = NavType.IntType
                            defaultValue = 0
                        },
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = "Check"
                        }
                    )
                ) { backStackEntry ->
                    val label = backStackEntry.arguments?.getString("label") ?: "Roll"
                    val modifier = backStackEntry.arguments?.getInt("modifier") ?: 0
                    val proficiencyBonus = backStackEntry.arguments?.getInt("proficiencyBonus") ?: 0
                    val type = backStackEntry.arguments?.getString("type") ?: "Check"

                    DiceRollScreen(
                        rollLabel = label,
                        modifier = modifier,
                        proficiencyBonus = proficiencyBonus,
                        type = type,
                        navController = navController,
                        chatViewModel = chatViewModel,
                        diceRollViewModel = diceRollViewModel
                    )
                }
            }
        }
    }
}
