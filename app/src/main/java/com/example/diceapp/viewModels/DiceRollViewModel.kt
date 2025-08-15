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
    fun rollHitDice(hitDieType: Int, conModifier: Int): String {
        val roll = Random.nextInt(1, hitDieType + 1)
        val total = roll + conModifier

        val prefix = if (messageMode == MessageMode.ToDM) "/ToDM " else ""

        return buildString {
            append("$prefix🎲 Hit Dice: Rolled 1d$hitDieType ($roll)")
            if (conModifier != 0) append(" ${if (conModifier > 0) "+$conModifier" else "$conModifier"}")
            append("\n= $total")
        }
    }
    fun rollDamage(label: String, damageDice: String, damageModifier: Int, damageType: String): String {
        val baseRoll = rollDiceExpression(damageDice)
        val total = baseRoll + damageModifier
        return "⚔\uFE0F $label : Rolled $damageDice ($baseRoll) $damageType \n= $baseRoll ${if (damageModifier >= 0) "+$damageModifier" else "$damageModifier"} \n= $total "
    }
    fun rollDiceExpression(expression: String): Int {
        val diceRegex = Regex("""(\d*)d(\d+)""")
        var total = 0

        val parts = expression.split('+', '-').map { it.trim() }
        val operators = Regex("[+-]").findAll(expression).map { it.value }.toList()

        var currentOpIndex = 0
        for ((index, part) in parts.withIndex()) {
            val op = if (index == 0) "+" else operators.getOrElse(currentOpIndex++) { "+" }

            val diceMatch = diceRegex.matchEntire(part)
            val value = if (diceMatch != null) {
                val count = diceMatch.groupValues[1].toIntOrNull() ?: 1
                val sides = diceMatch.groupValues[2].toInt()
                (1..count).sumOf { (1..sides).random() }
            } else {
                part.toIntOrNull() ?: 0
            }

            total += if (op == "+") value else -value
        }

        return total
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
