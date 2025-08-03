package com.example.diceapp.util

import kotlin.random.Random

fun parseAndRollDice(command: String): String {
    val regex = Regex("""(\d*)d(\d+)([+-]\d+)?""")
    val match = regex.matchEntire(command)

    if (match == null) {
        return "⚠️ Invalid dice format. Use like: 1d20+5"
    }

    val (diceCountStr, sidesStr, modifierStr) = match.destructured
    val diceCount = diceCountStr.toIntOrNull() ?: 1
    val sides = sidesStr.toInt()
    val modifier = modifierStr.toIntOrNull() ?: 0

    if (diceCount <= 0 || sides <= 0) {
        return "⚠️ Dice count and sides must be positive."
    }

    val rolls = List(diceCount) { Random.nextInt(1, sides + 1) }
    val total = rolls.sum() + modifier

    return buildString {
        append("🎲 Rolling ${diceCount}d$sides")
        if (modifier != 0) append(modifierStr)
        append(": ${rolls.joinToString(", ")}")
        if (modifier != 0) append(" + $modifier")
        append("\n= $total") // result on new line, for large styling
    }
}
