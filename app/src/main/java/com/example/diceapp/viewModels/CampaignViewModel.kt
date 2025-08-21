package com.example.diceapp.viewModels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diceapp.models.Campaign
import com.example.diceapp.models.CreateCampaignRequest
import com.example.diceapp.models.JoinCampaignRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "CampaignViewModel"

class CampaignViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var campaigns by mutableStateOf<List<Campaign>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var selectedCampaign by mutableStateOf<Campaign?>(null)
        private set

    fun loadCampaigns(context: Context) {
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
                    .url("$baseUrl/campaigns")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        campaigns = json.decodeFromString<List<Campaign>>(responseBody)
                        Log.d(TAG, "Loaded ${campaigns.size} campaigns")
                    }
                } else {
                    errorMessage = "Failed to load campaigns: ${response.code}"
                    Log.e(TAG, "Failed to load campaigns: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading campaigns", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun createCampaign(
        context: Context,
        name: String,
        description: String,
        maxPlayers: Int,
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

                val createRequest = CreateCampaignRequest(name, description, maxPlayers)
                val requestBody = json.encodeToString(createRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/campaigns")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "Campaign created successfully")
                    loadCampaigns(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to create campaign: ${response.code}"
                    Log.e(TAG, "Failed to create campaign: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error creating campaign", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun joinCampaign(context: Context, campaignId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val token = getToken(context)
                if (token == null) {
                    errorMessage = "Not logged in"
                    return@launch
                }

                val joinRequest = JoinCampaignRequest(campaignId)
                val requestBody = json.encodeToString(joinRequest)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/campaigns/join")
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "Joined campaign successfully")
                    loadCampaigns(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to join campaign: ${response.code}"
                    Log.e(TAG, "Failed to join campaign: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error joining campaign", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteCampaign(context: Context, campaignId: String, onSuccess: () -> Unit = {}) {
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
                    .url("$baseUrl/campaigns/$campaignId")
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "Campaign deleted successfully")
                    loadCampaigns(context)
                    onSuccess()
                } else {
                    errorMessage = "Failed to delete campaign: ${response.code}"
                    Log.e(TAG, "Failed to delete campaign: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error deleting campaign", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun selectCampaign(campaign: Campaign) {
        selectedCampaign = campaign
    }

    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
}