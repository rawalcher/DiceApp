package com.example.diceapp.viewModels

import androidx.lifecycle.ViewModel
import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DiceRollViewModel : ViewModel() {

    enum class RollMode { Normal, Advantage, Disadvantage }
    enum class MessageMode { Public, ToDM }

    var messageMode by mutableStateOf(MessageMode.Public)
    var rollMode by mutableStateOf(RollMode.Normal)

    fun rollDeathSave(): Int {
        return rollD20()
    }
    fun rollAndFormatMessage(
        label: String,
        modifier: Int,
        proficiencyBonus: Int? = null,
        type: String = "Check"
    ): String {
        val rolls = when (rollMode) {
            RollMode.Normal -> listOf(rollD20())
            RollMode.Advantage -> listOf(rollD20(), rollD20())
            RollMode.Disadvantage -> listOf(rollD20(), rollD20())
        }

        val selectedRoll = when (rollMode) {
            RollMode.Normal -> rolls[0]
            RollMode.Advantage -> rolls.max()
            RollMode.Disadvantage -> rolls.min()
        }

        val rollTypeLabel = when (rollMode) {
            RollMode.Advantage -> "Adv."
            RollMode.Disadvantage -> "Dis."
            RollMode.Normal -> "Normal"
        }

        val prof = proficiencyBonus ?: 0
        val total = selectedRoll + modifier + prof
        val prefix = if (messageMode == MessageMode.ToDM) "/ToDM " else ""

        return buildString {
            // First line: roll info
            append("$prefix🎲 $label $type: ")
            when (rollMode) {
                RollMode.Normal -> append("Rolled 1d20 ($selectedRoll) $rollTypeLabel")
                else -> append("Rolled 1d20 (${rolls.joinToString(" vs ")}) $selectedRoll $rollTypeLabel")
            }

            // Second line: calculation
            append("\n= $selectedRoll")
            if (modifier != 0) {
                append(" ${if (modifier > 0) "+$modifier" else "$modifier"}")
            }
            if (prof > 0) {
                append(" + $prof")
            }

            // Third line: total
            append("\n= $total")
        }
    }

    private fun rollD20(): Int = Random.nextInt(1, 21)
}
