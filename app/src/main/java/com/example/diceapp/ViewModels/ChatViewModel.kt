package com.example.diceapp.ViewModels

import androidx.lifecycle.ViewModel
import com.example.diceapp.util.parseAndRollDice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    fun postRollCommand(command: String) {
        if (!command.startsWith("/r ")) {
            addMessage("You: $command")
            return
        }
        val result = parseAndRollDice(command.removePrefix("/r ").trim())
        addMessage(result)
    }

    fun postMessage(text: String) {
        addMessage("You: $text")
    }

    fun postExternalRoll(description: String) {
        addMessage(description)
    }

    fun addMessage(message: String) {
        _messages.value = _messages.value + message
    }
}
