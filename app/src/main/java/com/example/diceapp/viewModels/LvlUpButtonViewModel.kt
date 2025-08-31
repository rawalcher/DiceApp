package com.example.diceapp.viewModels

import android.content.Context
import android.util.Base64
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "LvlUpButtonVM"

class LvlUpButtonViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var ownedCampaigns by mutableStateOf<List<Campaign>>(emptyList())
        private set

    var selectedCampaign: Campaign? by mutableStateOf(null)
        private set

    var characters by mutableStateOf<List<CampaignCharacter>>(emptyList())
        private set

    @Serializable
    data class LevelUpResponse(val updated: Int)


    fun levelUpCampaign(
        context: Context,
        campaignId: String,
        onResult: (updated: Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val token = getToken(context) ?: run {
                    val msg = "Not logged in"
                    errorMessage = msg
                    onError(msg)
                    return@launch
                }

                val req = Request.Builder()
                    .url("$baseUrl/campaigns/$campaignId/levelup")
                    .header("Authorization", "Bearer $token")
                    .post("".toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                if (!resp.isSuccessful) {
                    val msg = "Level up failed: ${resp.code}"
                    errorMessage = msg
                    onError(msg)
                    return@launch
                }

                val result = json.decodeFromString<LevelUpResponse>(resp.body?.string().orEmpty())
                loadCharactersForCampaign(context, campaignId)
                onResult(result.updated)
            } catch (t: Throwable) {
                val msg = "Error: ${t.localizedMessage}"
                Log.e(TAG, "levelUpCampaign", t)
                errorMessage = msg
                onError(msg)
            } finally {
                isLoading = false
            }
        }
    }

    fun loadCampaigns(context: Context, autoSelectFirst: Boolean = true) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val token = getToken(context) ?: run {
                    errorMessage = "Not logged in"; return@launch
                }

                val req = Request.Builder()
                    .url("$baseUrl/campaigns")
                    .header("Authorization", "Bearer $token")
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                if (!resp.isSuccessful) {
                    errorMessage = "Failed to load campaigns: ${resp.code}"
                    return@launch
                }

                val all = json.decodeFromString<List<Campaign>>(resp.body?.string().orEmpty())
                val me = parseMeFromJwt(token)
                ownedCampaigns = if (me != null) all.filter { it.ownerId == me.userId } else emptyList()

                if (autoSelectFirst && selectedCampaign == null && ownedCampaigns.isNotEmpty()) {
                    onSelectCampaign(context, ownedCampaigns.first())
                }
            } catch (t: Throwable) {
                Log.e(TAG, "loadCampaigns", t)
                errorMessage = "Error: ${t.localizedMessage}"
            } finally { isLoading = false }
        }
    }

    fun onSelectCampaign(context: Context, campaign: Campaign) {
        selectedCampaign = campaign
        loadCharactersForCampaign(context, campaign.id)
    }

    fun loadCharactersForCampaign(context: Context, campaignId: String) {
        viewModelScope.launch {
            isLoading = true; errorMessage = null
            try {
                val token = getToken(context) ?: run {
                    errorMessage = "Not logged in"; return@launch
                }

                val req = Request.Builder()
                    .url("$baseUrl/campaigns/$campaignId/characters")
                    .header("Authorization", "Bearer $token")
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                if (!resp.isSuccessful) {
                    errorMessage = "Failed to load characters: ${resp.code}"
                    return@launch
                }

                characters = json.decodeFromString(resp.body?.string().orEmpty())
            } catch (t: Throwable) {
                Log.e(TAG, "loadCharactersForCampaign", t)
                errorMessage = "Error: ${t.localizedMessage}"
            } finally { isLoading = false }
        }
    }

    // ---- helpers ----
    private fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_AUTH_TOKEN, null)

    private data class Me(val userId: String, val username: String)
    private fun parseMeFromJwt(token: String): Me? = try {
        val payloadB64 = token.split(".").getOrNull(1) ?: return null
        val decoded = String(Base64.decode(payloadB64, Base64.URL_SAFE or Base64.NO_WRAP))
        val obj = json.parseToJsonElement(decoded).jsonObject
        val userId = obj["userId"]?.jsonPrimitive?.content ?: return null
        val username = obj["username"]?.jsonPrimitive?.content ?: return null
        Me(userId, username)
    } catch (_: Throwable) { null }
}


@Serializable
data class CampaignCharacter(
    val id: Int,
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null
)
