package com.example.diceapp.viewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

enum class AppliesTo { ATTACK, SAVE, CHECK, ALL }

data class RollModifier(
    val name: String,
    val flatBonus: Int = 0,
    val extraDice: String? = null,
    val appliesTo: AppliesTo = AppliesTo.ALL,
    val enabled: Boolean = true
)

class ModifierViewModel : ViewModel() {

    val modifiers = mutableStateListOf<RollModifier>()

    fun addModifier(mod: RollModifier) { modifiers.add(mod) }
    fun removeModifier(index: Int) { if (index in modifiers.indices) modifiers.removeAt(index) }
    fun toggleEnabled(index: Int) {
        if (index in modifiers.indices) {
            val m = modifiers[index]
            modifiers[index] = m.copy(enabled = !m.enabled)
        }
    }
    fun aggregateFor(typeString: String): Pair<Int, List<String>> {
        val t = when (typeString.lowercase()) {
            "attack", "spell" -> AppliesTo.ATTACK
            "save", "saving throw" -> AppliesTo.SAVE
            else -> AppliesTo.CHECK
        }
        var flat = 0
        val dice = mutableListOf<String>()
        modifiers.forEach { m ->
            if (m.enabled && (m.appliesTo == AppliesTo.ALL || m.appliesTo == t)) {
                flat += m.flatBonus
                m.extraDice?.takeIf { it.isNotBlank() }?.let { dice += it }
            }
        }
        return flat to dice
    }
}
