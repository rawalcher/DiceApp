package com.example.diceapp.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.diceapp.viewModels.CharacterViewModel
import com.example.diceapp.viewModels.ChatViewModel
import com.example.diceapp.ui.theme.DiceAppTheme
import com.example.diceapp.viewModels.DiceRollViewModel
import com.example.diceapp.viewModels.CampaignViewModel
import com.example.diceapp.viewModels.AttackViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val characterViewModel: CharacterViewModel by viewModels()
    private val diceRollViewModel: DiceRollViewModel by viewModels()
    private val campaignViewModel: CampaignViewModel by viewModels()
    private val attackViewModel: AttackViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiceAppTheme {
                Surface {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        chatViewModel = chatViewModel,
                        characterViewModel = characterViewModel,
                        diceRollViewModel = diceRollViewModel,
                        campaignViewModel = campaignViewModel,
                        attackViewModel = attackViewModel
                    )
                }
            }
        }
    }
}