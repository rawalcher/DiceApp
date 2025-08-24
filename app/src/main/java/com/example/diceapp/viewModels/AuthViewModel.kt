package com.example.diceapp.viewModels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"

class AuthViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()

    var isAuthenticated by mutableStateOf(false)
        private set

    var isCheckingAuth by mutableStateOf(true)
        private set

    fun checkAuthentication(context: Context, onAuthenticated: () -> Unit, onNotAuthenticated: () -> Unit) {
        viewModelScope.launch {
            isCheckingAuth = true
            val token = getToken(context)

            if (token == null) {
                isAuthenticated = false
                isCheckingAuth = false
                onNotAuthenticated()
                return@launch
            }

            try {
                val request = Request.Builder()
                    .url("$baseUrl/me")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    isAuthenticated = true
                    onAuthenticated()
                } else {
                    isAuthenticated = false
                    clearToken(context)
                    onNotAuthenticated()
                }
            } catch (_: Exception) {
                isAuthenticated = false
                onNotAuthenticated()
            } finally {
                isCheckingAuth = false
            }
        }
    }

    fun onLoginSuccess() {
        isAuthenticated = true
    }

    fun logout(context: Context) {
        clearToken(context)
        isAuthenticated = false
    }

    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    private fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }
}