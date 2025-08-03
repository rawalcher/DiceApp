package com.example.diceapp.ViewModels

import androidx.lifecycle.ViewModel

data class Ability(val name: String, val value: Int) {
    val modifier: Int
        get() = (value - 10) / 2
}

class CharacterViewModel : ViewModel() {

    val abilities = listOf(
        Ability("Strength", 16),
        Ability("Dexterity", 14),
        Ability("Constitution", 13),
        Ability("Intelligence", 10),
        Ability("Wisdom", 8),
        Ability("Charisma", 12)
    )
}
