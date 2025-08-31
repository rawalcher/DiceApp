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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import androidx.core.content.edit

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "CreateCharacterVM"

class ManageCharacterViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

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

    private fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_TOKEN, null)

    private fun clearToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { remove(KEY_AUTH_TOKEN) }
    }

    private fun handleUnauthorized(context: Context) {
        errorMessage = "Session expired. Please log in again."
        clearToken(context)
    }

    private fun requireToken(context: Context): String {
        return getToken(context) ?: throw IllegalStateException("Not logged in")
    }

    private suspend fun authorizedRequest(
        context: Context,
        url: String,
        method: String = "GET",
        body: RequestBody? = null
    ): Response {
        val token = requireToken(context)
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .method(method, body)
            .build()
        return withContext(Dispatchers.IO) { client.newCall(req).execute() }
    }

    fun loadCharacters(context: Context) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val resp = authorizedRequest(context, "$baseUrl/characters")
                if (!resp.isSuccessful) error("Failed to load characters: ${resp.code}")
                val body = resp.body?.string() ?: error("Empty body")
                characters = json.decodeFromString(body)
                Log.d(TAG, "Loaded ${characters.size} characters")
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading characters", e)
            } finally { isLoading = false }
        }
    }

    fun loadCampaigns(context: Context) {
        viewModelScope.launch {
            errorMessage = null
            try {
                val resp = authorizedRequest(context, "$baseUrl/campaigns")
                if (!resp.isSuccessful) error("Failed to load campaigns: ${resp.code}")
                val body = resp.body?.string() ?: error("Empty body")
                campaigns = json.decodeFromString(body)
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading campaigns", e)
            }
        }
    }
    fun assignCharacterToCampaign(
        context: Context,
        characterId: Int,
        campaign: Campaign,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isAssigning = true; errorMessage = null
            try {
                val resp = authorizedRequest(
                    context,
                    "$baseUrl/characters/$characterId/assign/${campaign.id}",
                    "PUT",
                    "".toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                val ok = when {
                    resp.isSuccessful -> true
                    resp.code == 401 -> { handleUnauthorized(context); false }
                    resp.code == 409 -> { errorMessage = "Please leave the current campaign first."; false }
                    resp.code == 403 -> { errorMessage = "You do not have permission for this campaign."; false }
                    resp.code == 404 -> { errorMessage = "Character not found."; false }
                    else -> { errorMessage = "Failed to assign: ${resp.code}"; false }
                }
                if (ok) {
                    characters = characters.map { c ->
                        if (c.id == characterId) c.copy(
                            campaignId = campaign.id,
                            campaignName = campaign.name
                        ) else c
                    }
                    onSuccess()
                }
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error assigning character", e)
            } finally { isAssigning = false }
        }
    }

    fun unassignCharacter(context: Context, characterId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val resp = authorizedRequest(
                    context,
                    "$baseUrl/characters/$characterId/unassign",
                    "PUT",
                    "".toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                val ok = when {
                    resp.isSuccessful -> true
                    resp.code == 401 -> { handleUnauthorized(context); false }
                    else -> { errorMessage = "Failed to unassign: ${resp.code}"; false }
                }
                if (ok) {
                    characters = characters.map { c ->
                        if (c.id == characterId) c.copy(campaignId = null, campaignName = null) else c
                    }
                    onSuccess()
                }
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error unassigning character", e)
            } finally { isLoading = false }
        }
    }

    fun deleteCharacter(context: Context, characterId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val resp = authorizedRequest(context, "$baseUrl/characters/$characterId", "DELETE")
                val ok = when {
                    resp.isSuccessful -> true
                    resp.code == 401 -> { handleUnauthorized(context); false }
                    else -> { errorMessage = "Failed to delete character: ${resp.code}"; false }
                }
                if (ok) {
                    characters = characters.filterNot { it.id == characterId }
                    onSuccess()
                }
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error deleting character", e)
            } finally { isLoading = false }
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
            isLoading = true; errorMessage = null
            try {
                val body = CreateCharacterRequest(
                    name, charClass, level, raceName, raceDescription,
                    classDescription, appearanceDescription, backstory,
                    source.draftStr, source.draftDex, source.draftCon,
                    source.draftInt, source.draftWis, source.draftCha,
                    source.armorClass, source.maxHP, source.currentHP,
                    source.speed, source.proficiencyBonus,
                    source.hitDiceTotal, source.remainingHitDice,
                    source.hitDieType
                )
                val requestBody = json.encodeToString(CreateCharacterRequest.serializer(), body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val resp = authorizedRequest(context, "$baseUrl/characters", "POST", requestBody)
                val created: Character? = when {
                    resp.isSuccessful -> {
                        val bodyStr = resp.body?.string() ?: error("Empty body")
                        json.decodeFromString<Character>(bodyStr)
                    }
                    resp.code == 401 -> { handleUnauthorized(context); null }
                    else -> { errorMessage = "Failed to create character: ${resp.code}"; null }
                }

                if (created != null) {
                    characters = characters + created
                    onSuccess()
                }
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error creating character", e)
            } finally { isLoading = false }
        }
    }

    fun updateCharacter(
        context: Context,
        characterId: Int,
        source: CharacterViewModel,
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
            isLoading = true; errorMessage = null
            try {
                val body = UpdateCharacterRequest(
                    name, charClass, level, raceName, raceDescription,
                    classDescription, appearanceDescription, backstory,
                    source.draftStr, source.draftDex, source.draftCon,
                    source.draftInt, source.draftWis, source.draftCha,
                    source.armorClass, source.maxHP, source.currentHP,
                    source.speed, source.proficiencyBonus,
                    source.hitDiceTotal, source.remainingHitDice,
                    source.hitDieType
                )
                val requestBody = json.encodeToString(UpdateCharacterRequest.serializer(), body)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val resp = authorizedRequest(context, "$baseUrl/characters/$characterId", "PUT", requestBody)
                val updated: Character? = when {
                    resp.isSuccessful -> {
                        val bodyStr = resp.body?.string().orEmpty()
                        json.decodeFromString<Character>(bodyStr)
                    }
                    resp.code == 401 -> { handleUnauthorized(context); null }
                    resp.code == 403 -> { errorMessage = "You do not have permission to edit this character."; null }
                    else -> { errorMessage = "Failed to save: ${resp.code}"; null }
                }

                if (updated != null) {
                    characters = characters.map { if (it.id == updated.id) updated else it }
                    onSuccess(updated)
                }
            } catch (e: IllegalStateException) {
                errorMessage = e.message
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error updating character", e)
            } finally { isLoading = false }
        }
    }
}

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
    val campaignName: String? = null,
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val armorClass: Int = 10,
    val maxHp: Int = 1,
    val currentHp: Int = 1,
    val speed: Int = 30,
    val proficiencyBonus: Int = 2,
    val hitDiceTotal: Int = 1,
    val hitDiceRemaining: Int = 1,
    val hitDieType: Int = 6
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
