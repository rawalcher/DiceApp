package com.example.diceapp.ViewModels

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
}
