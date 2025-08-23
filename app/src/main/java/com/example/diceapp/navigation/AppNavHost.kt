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
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.screens.PlayScreen
import com.example.diceapp.screens.DiceRollScreen
import com.example.diceapp.screens.MainMenuScreen
import com.example.diceapp.screens.StatsScreen
import com.example.diceapp.screens.SavingThrowsScreen
import com.example.diceapp.screens.SkillsScreen
import com.example.diceapp.screens.CombatStatsScreen
import com.example.diceapp.screens.LoginScreen
import com.example.diceapp.viewModels.CampaignViewModel
import com.example.diceapp.screens.CampaignsScreen
import com.example.diceapp.screens.AttackScreen
import com.example.diceapp.viewModels.AttackViewModel
import com.example.diceapp.screens.AddAttackScreen
import com.example.diceapp.screens.CreateCharacterScreen
import com.example.diceapp.screens.SpellsScreen
import com.example.diceapp.viewModels.SpellViewModel
import com.example.diceapp.screens.AddSpellScreen
import com.example.diceapp.viewModels.Spell
import com.example.diceapp.screens.AdditionalResourcesScreen
import com.example.diceapp.screens.AddResourceScreen
import com.example.diceapp.viewModels.ResourceViewModel
import com.example.diceapp.screens.AdditionalModifiersScreen
import com.example.diceapp.screens.AddModifierScreen
import com.example.diceapp.screens.LvlUpButtonScreen
import com.example.diceapp.viewModels.CreateCharacterViewModel
import com.example.diceapp.viewModels.LvlUpButtonViewModel
import com.example.diceapp.viewModels.ModifierViewModel


@Composable
fun AppNavHost(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    characterViewModel: CharacterViewModel,
    diceRollViewModel: DiceRollViewModel,
    campaignViewModel: CampaignViewModel,
    attackViewModel: AttackViewModel,
    spellViewModel: SpellViewModel,
    resourceViewModel: ResourceViewModel,
    modifierViewModel: ModifierViewModel,
    createCharacterViewModel: CreateCharacterViewModel,
    lvlUpButtonViewModel: LvlUpButtonViewModel
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
                    PlayScreen(chatViewModel = chatViewModel)
                }
                composable(
                    route = "chat/{campaignId}",
                    arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val campaignId = backStackEntry.arguments?.getString("campaignId")
                    PlayScreen(
                        chatViewModel = chatViewModel,
                        campaignId = campaignId
                    )
                }
                composable("saving_throws") {
                    SavingThrowsScreen(
                        navController = navController,
                        characterViewModel = characterViewModel
                    )
                }

                composable("create_character") {
                    CreateCharacterScreen(
                        navController = navController,
                        viewModel = createCharacterViewModel
                    )
                }

                composable("dm_level_up") {
                    LvlUpButtonScreen( // oder LvlUpScreen
                        navController = navController,
                        viewModel = lvlUpButtonViewModel
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
                        chatViewModel = chatViewModel,
                        diceRollViewModel = diceRollViewModel,
                        resourceViewModel = resourceViewModel,
                        spellViewModel = spellViewModel,
                        onNavigateToChat = { navController.navigate("chat") }
                    )
                }

                composable("campaigns") {
                    CampaignsScreen(
                        navController = navController,
                        campaignViewModel = campaignViewModel
                    )
                }
                composable("login") {
                    LoginScreen(
                        onLoggedIn = {
                            navController.navigate("menu") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("attack") {
                    AttackScreen(
                        attackViewModel = attackViewModel,
                        diceRollViewModel = diceRollViewModel,
                        chatViewModel = chatViewModel,
                        navController = navController,
                    )
                }
                composable("spells") {
                    SpellsScreen(
                        navController = navController,
                        spellViewModel = spellViewModel,
                        chatViewModel = chatViewModel,
                        diceRollViewModel = diceRollViewModel,
                        spellSaveDC = characterViewModel.spellSaveDC
                    )
                }
                composable("resources") {
                    AdditionalResourcesScreen(
                        navController = navController,
                        resourceViewModel = resourceViewModel
                    )
                }
                composable("modifiers") {
                    AdditionalModifiersScreen(navController = navController, modifierViewModel = modifierViewModel)
                }
                composable("add_modifier") {
                    AddModifierScreen(navController = navController, modifierViewModel = modifierViewModel)
                }

                composable("add_resource") {
                    AddResourceScreen(
                        navController = navController,
                        resourceViewModel = resourceViewModel
                    )
                }

                composable("add_attack") {
                    AddAttackScreen(
                        navController = navController,
                        abilities = characterViewModel.abilities,
                        proficiencyBonus = characterViewModel.proficiencyBonus,
                        onAddAttack = { name, toHit, damageDice, damageModifier, damageType ->
                            attackViewModel.addAttack(name, toHit, damageDice, damageModifier, damageType)
                        }
                    )
                }
                composable("add_spell") {
                    AddSpellScreen(
                        navController = navController,
                        abilities = characterViewModel.abilities,
                        proficiencyBonus = characterViewModel.proficiencyBonus,
                        spellSaveDC = characterViewModel.spellSaveDC,
                        onAddSpell = { name, level, castingTime, range, components,
                                       concentration, ritual, attackBonus, saveDC, saveAbility,
                                       damageDice, damageModifier, damageType, description ->
                            spellViewModel.addSpell(
                                Spell(
                                    name = name,
                                    level = level,
                                    castingTime = castingTime,
                                    range = range,
                                    components = components,
                                    concentration = concentration,
                                    ritual = ritual,
                                    attackBonus = attackBonus,
                                    saveDC = saveDC,
                                    saveAbility = saveAbility,
                                    damageDice = damageDice,
                                    damageModifier = damageModifier,
                                    damageType = damageType,
                                    description = description
                                )
                            )
                        }
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
                        diceRollViewModel = diceRollViewModel,
                        modifierViewModel = modifierViewModel
                    )
                }
            }
        }
    }
}