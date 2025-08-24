package com.example.diceapp.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.diceapp.viewModels.AuthViewModel
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import androidx.core.content.edit

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "LoginScreen"

private fun saveToken(context: Context, token: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putString(KEY_AUTH_TOKEN, token) }
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val baseUrl = "http://10.0.2.2:8080" // Emulator localhost

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<String?>(null) } // "login" or "register"

    val scope = rememberCoroutineScope()

    fun performAuth(endpoint: String) {
        if (isLoading) return
        isLoading = true
        currentAction = endpoint

        scope.launch {
            try {
                val jsonObject = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }
                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string()
                    if (responseBodyString != null) {
                        try {
                            val token = JSONObject(responseBodyString).getString("token")
                            saveToken(context, token)
                            authViewModel.onLoginSuccess()
                            navController.navigate("menu") {
                                popUpTo("login") { inclusive = true }
                            }
                        } catch (e: JSONException) {
                            Log.e(TAG, "Error parsing token: $responseBodyString", e)
                            Toast.makeText(context, "Invalid response from server.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Empty response from server.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "{}").getString("message")
                    } catch (_: JSONException) {
                        response.message.ifEmpty { "${response.code}: Operation failed" }
                    }
                    Log.w(TAG, "Auth failed ($endpoint): $errorMessage")
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during auth ($endpoint)", e)
                Toast.makeText(context, "Error: ${e.localizedMessage ?: "Please check connection"}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                currentAction = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dice App Login", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { performAuth("/login") },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading && currentAction == "/login") {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { performAuth("/register") },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading && currentAction == "/register") {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Register")
            }
        }

        if (isLoading && currentAction == "/me") {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}