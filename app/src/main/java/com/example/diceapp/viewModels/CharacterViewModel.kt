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
        Skill("Acrobatics", "Dexterity", isProficient = true),
        Skill("Animal Handling", "Wisdom", isProficient = false),
        Skill("Arcana", "Intelligence", isProficient = false),
        Skill("Athletics", "Strength", isProficient = true),
        Skill("Deception", "Charisma", isProficient = false),
        Skill("History", "Intelligence", isProficient = true),
        Skill("Insight", "Wisdom", isProficient = false),
        Skill("Intimidation", "Charisma", isProficient = false),
        Skill("Investigation", "Intelligence", isProficient = true),
        Skill("Medicine", "Wisdom", isProficient = false),
        Skill("Nature", "Intelligence", isProficient = false),
        Skill("Perception", "Wisdom", isProficient = true),
        Skill("Performance", "Charisma", isProficient = false),
        Skill("Persuasion", "Charisma", isProficient = true),
        Skill("Religion", "Intelligence", isProficient = false),
        Skill("Sleight of Hand", "Dexterity", isProficient = false),
        Skill("Stealth", "Dexterity", isProficient = true),
        Skill("Survival", "Wisdom", isProficient = false)
    ).map { skill ->
        val abilityMod = abilities.find { it.name == skill.abilityName }?.modifier ?: 0
        val profMod = if (skill.isProficient) proficiencyBonus else 0
        skill.copy(modifier = abilityMod + profMod)
    }

    val armorClass: Int = 13
    val initiative: Int = abilities.find { it.name == "Dexterity" }?.modifier ?: 0
    var inspiration by mutableStateOf(false)

    val speed: Int = 30

    var currentHP by mutableStateOf(20)
    val maxHP: Int = 20
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
