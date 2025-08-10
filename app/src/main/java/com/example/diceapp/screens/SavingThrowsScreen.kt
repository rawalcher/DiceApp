package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.Ability
import com.example.diceapp.viewModels.CharacterViewModel

@Composable
fun SavingThrowsScreen(
    navController: NavController,
    characterViewModel: CharacterViewModel
) {
    val abilities = characterViewModel.abilities
    val proficiencyBonus = characterViewModel.proficiencyBonus

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            abilities.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { ability ->
                        val bonus = if (ability.isProficient) proficiencyBonus else 0
                        SavingThrowCard(
                            ability = ability,
                            proficiencyBonus = bonus,
                            onClick = {
                                navController.navigate(
                                    "dice_roll/${ability.name}/${ability.modifier}/$bonus/Save"
                                )

                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SavingThrowCard(
    ability: Ability,
    proficiencyBonus: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val totalModifier = ability.modifier + proficiencyBonus

    Card(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, Color.White, shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ability.name,
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = "${ability.value}",
                fontSize = 48.sp,
                color = Color.White
            )
            Text(
                text = (if (totalModifier >= 0) "+" else "") + totalModifier,
                fontSize = 32.sp,
                color = Color.White
            )
            if (ability.isProficient) {
                Text(
                    text = "Proficient (+$proficiencyBonus)",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}
