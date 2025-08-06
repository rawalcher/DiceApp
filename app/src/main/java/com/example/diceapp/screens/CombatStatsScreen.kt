package com.example.diceapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diceapp.ViewModels.CharacterViewModel
import androidx.compose.foundation.background

@Composable
fun CombatStatsScreen(characterViewModel: CharacterViewModel) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = Color.White
    val cardColor = Color.Black
    val textColor = Color.White

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard("Proficiency Bonus", "+${characterViewModel.proficiencyBonus}", shape, borderColor, cardColor, textColor, Modifier.weight(1f))
                StatCard("Passive Perception", characterViewModel.passivePerception.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard("Armor Class", characterViewModel.armorClass.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
                StatCard("Max HP", characterViewModel.maxHP.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
                StatCard("Speed", characterViewModel.speed.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
            }

            StatCard(
                title = "Current HP",
                value = characterViewModel.currentHP.toString(),
                shape = shape,
                borderColor = borderColor,
                cardColor = cardColor,
                textColor = textColor
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard("Initiative", characterViewModel.initiative.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
                StatCard("Temp HP", characterViewModel.tempHP.toString(), shape, borderColor, cardColor, textColor, Modifier.weight(1f))
            }

            StatCard(
                title = "Hit Dice",
                value = "${characterViewModel.hitDiceTotal} (d${characterViewModel.hitDieType})",
                shape = shape,
                borderColor = borderColor,
                cardColor = cardColor,
                textColor = textColor
            )

            DeathSavesSection(
                successes = characterViewModel.deathSaveSuccesses,
                failures = characterViewModel.deathSaveFailures
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { characterViewModel.shortRest() },) { Text("Short Rest") }
                Button(onClick = { characterViewModel.longRest() },) { Text("Long Rest") }
            }
        }
    }
}


@Composable
private fun StatCard(
    title: String,
    value: String,
    shape: RoundedCornerShape,
    borderColor: Color,
    cardColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        shape = shape,
        modifier = modifier
            .height(80.dp)
            .border(BorderStroke(2.dp, borderColor), shape),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title.uppercase(), fontSize = 12.sp, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, color = textColor)
        }
    }
}
@Composable
fun DeathSavesSection(
    successes: Int,
    failures: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Death Saves", fontSize = 18.sp, color = Color.White)

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Successes", fontSize = 14.sp, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        SaveCircle(
                            filled = index < successes,
                            filledColor = Color.Green
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Failures", fontSize = 14.sp, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        SaveCircle(
                            filled = index < failures,
                            filledColor = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveCircle(filled: Boolean, filledColor: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, Color.White, shape = RoundedCornerShape(50))
            .let {
                if (filled) it.background(filledColor, shape = RoundedCornerShape(50)) else it
            }
    )
}
