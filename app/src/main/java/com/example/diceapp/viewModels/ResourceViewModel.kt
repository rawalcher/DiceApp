package com.example.diceapp.viewModels

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.*

enum class ResetOn { SHORT_REST, LONG_REST, NONE }

class ResourceState(
    name: String,
    max: Int,
    current: Int,
    resetOn: ResetOn
) {
    var name by mutableStateOf(name)
    var max by mutableIntStateOf(max)
    var current by mutableIntStateOf(current)
    var resetOn by mutableStateOf(resetOn)
}

class ResourceViewModel : ViewModel() {
    val resources = mutableStateListOf<ResourceState>()

    fun addResource(name: String, max: Int, resetOn: ResetOn) {
        resources.add(ResourceState(name = name, max = max.coerceAtLeast(0), current = max.coerceAtLeast(0), resetOn = resetOn))
    }

    fun removeResource(index: Int) {
        if (index in resources.indices) resources.removeAt(index)
    }

    fun setCurrent(index: Int, value: Int) {
        val r = resources.getOrNull(index) ?: return
        r.current = value.coerceIn(0, r.max)
    }
    fun shortRest() {
        resources.forEach { if (it.resetOn == ResetOn.SHORT_REST) it.current = it.max }
    }

    fun longRest() {
        resources.forEach {
            if (it.resetOn == ResetOn.LONG_REST || it.resetOn == ResetOn.SHORT_REST) {
                it.current = it.max
            }
        }
    }
}