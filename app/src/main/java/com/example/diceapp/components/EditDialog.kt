package com.example.diceapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.text.iterator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

@Composable
fun EditDialog(
    fieldName: String,
    initialValue: Int,
    valueRange: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var input by remember { mutableStateOf(initialValue.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Field name
                Text(
                    text = fieldName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Displayed input value
                Text(
                    text = input.ifEmpty { "0" },
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Number buttons
                val numberPad = listOf(
                    listOf("7", "8", "9"),
                    listOf("4", "5", "6"),
                    listOf("1", "2", "3"),
                    listOf("0", "C", "←")
                )

                numberPad.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { label ->
                            Button(
                                onClick = {
                                    when (label) {
                                        "C" -> input = ""
                                        "←" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                        else -> input += label
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) {
                                Text(text = label, color = Color.White, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Row for + and -
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (input.isNotEmpty() && input.last().isDigit()) {
                                input += "+"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("+", fontSize = 20.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (input.isNotEmpty() && input.last().isDigit()) {
                                input += "-"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("-", fontSize = 20.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("✕", fontSize = 20.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val result = try {
                                evalExpression(input).coerceIn(valueRange)
                            } catch (e: Exception) {
                                initialValue
                            }
                            onApply(result)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("✓", fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

fun evalExpression(expression: String): Int {
    val sanitized = expression.replace(Regex("[^0-9+-]"), "")
    if (sanitized.isBlank()) return 0

    val tokens = mutableListOf<String>()
    var number = ""

    for (char in sanitized) {
        if (char == '+' || char == '-') {
            if (number.isNotEmpty()) {
                tokens.add(number)
                number = ""
            }
            tokens.add(char.toString())
        } else {
            number += char
        }
    }

    if (number.isNotEmpty()) tokens.add(number)

    var total = 0
    var sign = 1

    for (token in tokens) {
        when (token) {
            "+" -> sign = 1
            "-" -> sign = -1
            else -> {
                val num = token.toIntOrNull() ?: 0
                total += sign * num
            }
        }
    }

    return total
}