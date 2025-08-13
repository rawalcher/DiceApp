package com.example.diceapp.viewModels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

data class Ability(
    val name: String,
    val value: Int,
    val isProficient: Boolean = false
) {
    val modifier: Int
        get() = (value - 10) / 2
}

data class Skill(
    val name: String,
    val abilityName: String,
    val isProficient: Boolean,
    val modifier: Int = 0
)

class CharacterViewModel : ViewModel() {

    val proficiencyBonus = 2

    val abilities = listOf(
        Ability("Strength", 16, isProficient = true),
        Ability("Dexterity", 14),
        Ability("Constitution", 13, isProficient = true),
        Ability("Intelligence", 10),
        Ability("Wisdom", 8),
        Ability("Charisma", 12)
    )

    val skills: List<Skill> = listOf(
        Skill("Acrobatics", "Dexterity", true),
        Skill("Animal Handling", "Wisdom", false),
        Skill("Arcana", "Intelligence", false),
        Skill("Athletics", "Strength", true),
        Skill("Deception", "Charisma", false),
        Skill("History", "Intelligence", true),
        Skill("Insight", "Wisdom", false),
        Skill("Intimidation", "Charisma", false),
        Skill("Investigation", "Intelligence", true),
        Skill("Medicine", "Wisdom", false),
        Skill("Nature", "Intelligence", false),
        Skill("Perception", "Wisdom", true),
        Skill("Performance", "Charisma", false),
        Skill("Persuasion", "Charisma", true),
        Skill("Religion", "Intelligence", false),
        Skill("Sleight of Hand", "Dexterity", false),
        Skill("Stealth", "Dexterity", true),
        Skill("Survival", "Wisdom", false)
    ).map { skill ->
        val abilityMod = abilities.find { it.name == skill.abilityName }?.modifier ?: 0
        val profMod = if (skill.isProficient) proficiencyBonus else 0
        skill.copy(modifier = abilityMod + profMod)
    }

    var armorClass by mutableStateOf(13)
    val initiative: Int = abilities.find { it.name == "Dexterity" }?.modifier ?: 0
    var inspiration by mutableStateOf(false)

    var speed by mutableStateOf(30)

    var currentHP by mutableStateOf(20)
    var maxHP by mutableStateOf(20)

    fun updateMaxHP(newValue: Int) {
        maxHP = newValue
        currentHP = currentHP.coerceAtMost(maxHP)
    }

    var tempHP by mutableStateOf(0)

    val hitDiceTotal: Int = 3
    val hitDieType: Int = 6

    private val _deathSaveSuccesses = mutableStateOf(0)
    val deathSaveSuccesses: State<Int> get() = _deathSaveSuccesses

    private val _deathSaveFailures = mutableStateOf(0)
    val deathSaveFailures: State<Int> get() = _deathSaveFailures

    val passivePerception: Int
        get() {
            val wisdomMod = abilities.find { it.name == "Wisdom" }?.modifier ?: 0
            val isProficient = skills.find { it.name == "Perception" }?.isProficient ?: false
            return 10 + wisdomMod + if (isProficient) proficiencyBonus else 0
        }

    fun resetDeathSaves() {
        _deathSaveSuccesses.value = 0
        _deathSaveFailures.value = 0
    }

    fun shortRest() {
        tempHP = 0
        resetDeathSaves()
    }

    fun longRest() {
        currentHP = maxHP
        tempHP = 0
        resetDeathSaves()
    }

    fun applyDeathSaveResult(roll: Int, chatViewModel: ChatViewModel) {
        val message = "/ToDM 🎲 Death Save: Rolled 1d20 = $roll"
        chatViewModel.addMessage(message)

        when {
            roll == 1 -> {
                _deathSaveFailures.value = (_deathSaveFailures.value + 2).coerceAtMost(3)
            }
            roll < 10 -> {
                _deathSaveFailures.value = (_deathSaveFailures.value + 1).coerceAtMost(3)
            }
            roll == 20 -> {
                _deathSaveSuccesses.value = 3
            }
            else -> {
                _deathSaveSuccesses.value = (_deathSaveSuccesses.value + 1).coerceAtMost(3)
            }
        }
    }
}
