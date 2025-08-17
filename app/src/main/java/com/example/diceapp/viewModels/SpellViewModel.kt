package com.example.diceapp.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class Spell(
    val name: String,
    val level: Int,
    val castingTime: String,
    val range: String,
    val components: String,
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val prepared: Boolean = false,

    val attackBonus: Int? = null,
    val saveDC: Int? = null,
    val saveAbility: String? = null,

    val damageDice: String? = null,
    val damageModifier: Int = 0,
    val damageType: String? = null,

    val description: String = ""
)

class SpellViewModel : ViewModel() {
    val spells = mutableStateListOf<Spell>()
    fun addSpell(spell: Spell) {
        spells.add(spell)
    }
    fun removeSpell(index: Int) {
        if (index in spells.indices) spells.removeAt(index)
    }
    fun setPrepared(index: Int, prepared: Boolean) {
        if (index in spells.indices) {
            spells[index] = spells[index].copy(prepared = prepared)
        }
    }
    class SpellSlotState(val level: Int, max: Int, current: Int) {
        var max by mutableIntStateOf(max)
        var current by mutableIntStateOf(current)
    }
    val spellSlots = mutableStateListOf<SpellSlotState>().apply {
        for (lvl in 0..9) add(SpellSlotState(lvl, max = if (lvl == 0) 0 else 0, current = 0))
    }
    fun setMaxSlots(level: Int, max: Int) {
        val s = spellSlots.getOrNull(level) ?: return
        s.max = max.coerceAtLeast(0)
        s.current = s.current.coerceAtMost(s.max)
    }
    fun resetSlots(level: Int) {
        val s = spellSlots.getOrNull(level) ?: return
        if (s.level != 0) s.current = s.max
    }
    fun incrementSlot(level: Int) {
        val s = spellSlots.getOrNull(level) ?: return
        if (s.level != 0) s.current = (s.current + 1).coerceAtMost(s.max)
    }
    fun decrementSlot(level: Int) {
        val s = spellSlots.getOrNull(level) ?: return
        if (s.level != 0) s.current = (s.current - 1).coerceAtLeast(0)
    }
}
