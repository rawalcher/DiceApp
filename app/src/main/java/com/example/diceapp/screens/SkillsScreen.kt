package com.example.diceapp.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.CharacterViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun SkillsScreen(
    navController: NavController,
    characterViewModel: CharacterViewModel
) {
    val skills = characterViewModel.skills

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(skills) { skill ->
                val encodedLabel = URLEncoder.encode(skill.name, StandardCharsets.UTF_8.toString())
                SkillCard(
                    skillName = skill.name,
                    abilityAbbrev = skill.abilityName.take(3).uppercase(),
                    modifier = skill.modifier,
                    isProficient = skill.isProficient,
                    onClick = {
                        navController.navigate("dice_roll/$encodedLabel/${skill.modifier}/0/Check")
                    }
                )
            }
        }
    }
}

@Composable
fun SkillCard(
    skillName: String,
    abilityAbbrev: String,
    modifier: Int,
    isProficient: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(2.dp, Color.White, shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$skillName ($abilityAbbrev)",
                fontSize = 20.sp,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .then(
                        if (isProficient) {
                            Modifier
                                .border(2.dp, Color.White, CircleShape)
                                .padding(8.dp)
                        } else Modifier.padding(8.dp)
                    )
            ) {
                Text(
                    text = "${if (modifier >= 0) "+" else ""}$modifier",
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}
