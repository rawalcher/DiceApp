package com.example.diceapp.viewModels

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.collections.plus

class CreateCharacterViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var characters by mutableStateOf<List<Character>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var races by mutableStateOf(
        listOf(
            Race("Elf", "Edle Rasse, flink und gut mit Pfeil & Bogen"),
            Race("Zwerg", "Robust, gut im Bergbau und Nahkampf"),
            Race("Tiefling", "Dämonisches Erbe, magische Affinität"),
            Race("Mensch", "Anpassungsfähig und vielseitig")
        )
    )
        private set

    fun loadCharacters(context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val request = Request.Builder()
                    .url("$baseUrl/characters")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        characters = json.decodeFromString<List<Character>>(responseBody)
                        Log.d(TAG, "Loaded ${characters.size} characters")
                    }
                } else {
                    errorMessage = "Failed to load characters: ${response.code}"
                    Log.e(TAG, "Failed to load characters: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading characters", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun createCharacter(
        context: Context,
        name: String,
        charClass: String,
        level: Int,
        raceName: String?,
        raceDescription: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val createRequest = CreateCharacterRequest(name, charClass, level, raceName, raceDescription)
                val requestBody = json.encodeToString(createRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/characters")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "Character created successfully")
                    loadCharacters(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to create character: ${response.code}"
                    Log.e(TAG, "Failed to create character: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error creating character", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteCharacter(context: Context, characterId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val request = Request.Builder()
                    .url("$baseUrl/characters/$characterId")
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "Character deleted successfully")
                    loadCharacters(context) // Liste aktualisieren
                    onSuccess()
                } else {
                    errorMessage = "Failed to delete character: ${response.code}"
                    Log.e(TAG, "Failed to delete character: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error deleting character", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun addRace(name: String, description: String) {
        races = races + Race(name, description)
    }

    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
}

// Data classes
@Serializable
data class Character(
    val id: Int = 0,
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null
)
@Serializable
data class Race(
    val name: String,
    val description: String
)
@Serializable
data class CreateCharacterRequest(
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null
)