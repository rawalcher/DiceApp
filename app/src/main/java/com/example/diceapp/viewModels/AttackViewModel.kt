package com.example.diceapp.viewModels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
data class Attack(
    val name: String,
    val toHit: Int,
    val damageDice: String,
    val damageModifier: Int,
    val damageType: String
)
class AttackViewModel : ViewModel() {
    val _attacks = mutableStateListOf<Attack>()
    val attacks: List<Attack> = _attacks
    fun addAttack(name: String, toHit: Int, damageDice: String, damageModifier: Int, damageType: String) {
        val newAttack = Attack(name, toHit, damageDice, damageModifier, damageType)
        _attacks.add(newAttack)
    }
    fun removeAttack(index: Int) {
        _attacks.removeAt(index)
    }
}
