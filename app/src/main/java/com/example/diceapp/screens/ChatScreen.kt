package com.example.diceapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.diceapp.ViewModels.ChatViewModel

@Composable
fun ChatScreen(chatViewModel: ChatViewModel) {
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    val lines = message.split("\n")
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        lines.forEach { line ->
                            val isResultLine = line.trim().matches(Regex("""^\d+$"""))
                            Text(
                                text = line,
                                style = if (isResultLine)
                                    MaterialTheme.typography.headlineMedium
                                else
                                    MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("Type /r 1d20+5 or a message") },
                    singleLine = true
                )
                Button(onClick = {
                    if (inputText.isNotBlank()) {
                        if (inputText.startsWith("/r ")) {
                            chatViewModel.postRollCommand(inputText)
                        } else {
                            chatViewModel.postMessage(inputText)
                        }
                        inputText = ""
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}
