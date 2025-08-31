package com.example.diceapp.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.diceapp.viewModels.AuthViewModel
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.ui.theme.DiceAppTheme
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.viewModels.CampaignViewModel
import com.example.diceapp.viewModels.AttackViewModel
import com.example.diceapp.viewModels.ManageCharacterViewModel
import com.example.diceapp.viewModels.SpellViewModel
import com.example.diceapp.viewModels.ResourceViewModel
import com.example.diceapp.viewModels.ModifierViewModel
import com.example.diceapp.viewModels.LvlUpButtonViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val characterViewModel: CharacterViewModel by viewModels()
    private val diceRollViewModel: DiceRollViewModel by viewModels()
    private val campaignViewModel: CampaignViewModel by viewModels()
    private val attackViewModel: AttackViewModel by viewModels()
    private val spellViewModel: SpellViewModel by viewModels()
    private val resourceViewModel: ResourceViewModel by viewModels()
    private val modifierViewModel: ModifierViewModel by viewModels()
    private val manageCharacterViewModel: ManageCharacterViewModel by viewModels()
    private val lvlUpButtonViewModel: LvlUpButtonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiceAppTheme {
                Surface {
                    val navController = rememberNavController()

                    LaunchedEffect(Unit) {
                        authViewModel.checkAuthentication(
                            context = this@MainActivity,
                            onAuthenticated = {
                                if (navController.currentBackStackEntry?.destination?.route == "login") {
                                    navController.navigate("menu") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            },
                            onNotAuthenticated = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    AppNavHost(
                        navController = navController,
                        authViewModel = authViewModel,
                        chatViewModel = chatViewModel,
                        characterViewModel = characterViewModel,
                        diceRollViewModel = diceRollViewModel,
                        campaignViewModel = campaignViewModel,
                        attackViewModel = attackViewModel,
                        spellViewModel = spellViewModel,
                        resourceViewModel = resourceViewModel,
                        modifierViewModel = modifierViewModel,
                        manageCharacterViewModel = manageCharacterViewModel,
                        lvlUpButtonViewModel = lvlUpButtonViewModel
                    )
                }
            }
        }
    }
}