package com.example.diceapp.viewModels

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

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "CreateCharacterVM"

class CreateCharacterViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true

    }

    var campaigns by mutableStateOf<List<Campaign>>(emptyList())
        private set

    var isAssigning by mutableStateOf(false)
        private set

    var characters by mutableStateOf<List<Character>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // -------- Helpers --------
    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    private fun abilityValue(vm: CharacterViewModel, name: String): Int =
        vm.abilities.firstOrNull { it.name == name }?.value ?: 10

    // -------- API Calls --------

    fun loadCharacters(context: Context) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }
                val request = Request.Builder()
                    .url("$baseUrl/characters")
                    .header("Authorization", "Bearer $token")
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        characters = json.decodeFromString(body)
                        Log.d(TAG, "Loaded ${characters.size} characters")
                    }
                } else {
                    errorMessage = "Failed to load characters: ${response.code}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading characters", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loadCampaigns(context: Context) {
        viewModelScope.launch {
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }
                val request = Request.Builder()
                    .url("$baseUrl/campaigns")
                    .header("Authorization", "Bearer $token")
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        campaigns = json.decodeFromString(body)
                    }
                } else {
                    errorMessage = "Failed to load campaigns: ${response.code}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun assignCharacterToCampaign(
        context: Context,
        characterId: Int,
        campaignId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isAssigning = true
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }
                val request = Request.Builder()
                    .url("$baseUrl/characters/$characterId/assign/$campaignId")
                    .header("Authorization", "Bearer $token")
                    .put("".toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    loadCharacters(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to assign: ${response.code}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            } finally {
                isAssigning = false
            }
        }
    }

    fun unassignCharacter(
        context: Context,
        characterId: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }
                val request = Request.Builder()
                    .url("$baseUrl/characters/$characterId/unassign")
                    .header("Authorization", "Bearer $token")
                    .put("".toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    loadCharacters(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to unassign: ${response.code}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
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
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }
                val request = Request.Builder()
                    .url("$baseUrl/characters/$characterId")
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    loadCharacters(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to delete character: ${response.code}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }


    fun createCharacter(
        context: Context,
        source: CharacterViewModel,
        name: String,
        charClass: String,
        level: Int,
        raceName: String?,
        raceDescription: String?,
        classDescription: String? = null,
        appearanceDescription: String? = null,
        backstory: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }

                val createRequest = CreateCharacterRequest(
                    name = name,
                    charClass = charClass,
                    level = level,
                    raceName = raceName,
                    raceDescription = raceDescription,
                    classDescription = classDescription,
                    appearanceDescription = appearanceDescription,
                    backstory = backstory,

                    strength = abilityValue(source, "Strength"),
                    dexterity = abilityValue(source, "Dexterity"),
                    constitution = abilityValue(source, "Constitution"),
                    intelligence = abilityValue(source, "Intelligence"),
                    wisdom = abilityValue(source, "Wisdom"),
                    charisma = abilityValue(source, "Charisma"),

                    armorClass = source.armorClass,
                    maxHp = source.maxHP,
                    currentHp = source.currentHP,
                    speed = source.speed,
                    proficiencyBonus = source.proficiencyBonus,
                    hitDiceTotal = source.hitDiceTotal,
                    hitDiceRemaining = source.remainingHitDice,
                    hitDieType = source.hitDieType
                )

                val requestBody = json.encodeToString(CreateCharacterRequest.serializer(), createRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/characters")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
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


    fun updateCharacter(
        context: Context,
        characterId: Int,
        source: CharacterViewModel, // <- Werte aus deinem laufenden VM
        name: String,
        charClass: String,
        level: Int,
        raceName: String?,
        raceDescription: String?,
        classDescription: String?,
        appearanceDescription: String?,
        backstory: String?,
        onSuccess: (Character) -> Unit = {},
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = getToken(context) ?: run { errorMessage = "Not logged in"; return@launch }

                val body = UpdateCharacterRequest(
                    name = name,
                    charClass = charClass,
                    level = level,
                    raceName = raceName,
                    raceDescription = raceDescription,
                    classDescription = classDescription,
                    appearanceDescription = appearanceDescription,
                    backstory = backstory,

                    strength = abilityValue(source, "Strength"),
                    dexterity = abilityValue(source, "Dexterity"),
                    constitution = abilityValue(source, "Constitution"),
                    intelligence = abilityValue(source, "Intelligence"),
                    wisdom = abilityValue(source, "Wisdom"),
                    charisma = abilityValue(source, "Charisma"),

                    armorClass = source.armorClass,
                    maxHp = source.maxHP,
                    currentHp = source.currentHP,
                    speed = source.speed,
                    proficiencyBonus = source.proficiencyBonus,
                    hitDiceTotal = source.hitDiceTotal,
                    hitDiceRemaining = source.remainingHitDice,
                    hitDieType = source.hitDieType
                )

                val requestBody = json.encodeToString(UpdateCharacterRequest.serializer(), body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val req = Request.Builder()
                    .url("$baseUrl/characters/$characterId")
                    .header("Authorization", "Bearer $token")
                    .put(requestBody)
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                if (!resp.isSuccessful) {
                    errorMessage = "Failed to update character: ${resp.code}"
                    return@launch
                }

                val updated: Character = json.decodeFromString(resp.body?.string().orEmpty())
                loadCharacters(context)
                onSuccess(updated)
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error updating character", e)
            } finally {
                isLoading = false
            }
        }
    }
}

// --------------------- DTOs (Client) ---------------------

@Serializable
data class Character(
    val id: Int = 0,
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val classDescription: String? = null,
    val appearanceDescription: String? = null,
    val backstory: String? = null,
    val raceDescription: String? = null,
    val campaignId: String? = null,
    val campaignName: String? = null

)

@Serializable
data class Campaign(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val ownerName: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val isJoined: Boolean
)


@Serializable
data class CreateCharacterRequest(
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null,
    val classDescription: String? = null,
    val appearanceDescription: String? = null,
    val backstory: String? = null,

    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val armorClass: Int,
    val maxHp: Int,
    val currentHp: Int,
    val speed: Int,
    val proficiencyBonus: Int,
    val hitDiceTotal: Int,
    val hitDiceRemaining: Int,
    val hitDieType: Int
)


@Serializable
data class UpdateCharacterRequest(
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null,
    val classDescription: String? = null,
    val appearanceDescription: String? = null,
    val backstory: String? = null,

    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val armorClass: Int,
    val maxHp: Int,
    val currentHp: Int,
    val speed: Int,
    val proficiencyBonus: Int,
    val hitDiceTotal: Int,
    val hitDiceRemaining: Int,
    val hitDieType: Int
)
