package com.example.diceapp.ViewModels

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

    fun rollAndFormatMessage(
        label: String,
        modifier: Int,
        proficiencyBonus: Int? = null
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
            append("$prefix🎲 $label Check: ")

            when (rollMode) {
                RollMode.Normal -> append("Rolled 1d20 ($selectedRoll) $rollTypeLabel")
                else -> append("Rolled 1d20 (${rolls.joinToString(" vs ")}) $selectedRoll $rollTypeLabel")
            }

            if (modifier != 0 || prof > 0) {
                val modPart = if (modifier != 0) " + $modifier" else ""
                val profPart = if (prof > 0) " + $prof" else ""
                append(modPart + profPart)
            }

            append("\n= $total")
        }
    }


    private fun rollD20(): Int = Random.nextInt(1, 21)
}
