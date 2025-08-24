package com.example.diceapp.viewModels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diceapp.models.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private const val PREFS_NAME = "dice_app_prefs"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val TAG = "CharacterViewModel"

data class Ability(
    val name: String,
    val value: Int,
    val isProficient: Boolean = false
) {
    val modifier: Int
        get() = (value - 10) / 2
}

data class Skill(
    val name: String,
    val abilityName: String,
    val isProficient: Boolean,
    val modifier: Int = 0
)

@Serializable
data class CharacterData(
    val id: Int,
    val name: String,
    val charClass: String,
    val level: Int,
    val raceName: String? = null,
    val raceDescription: String? = null,
    val classDescription: String? = null,
    val appearanceDescription: String? = null,
    val backstory: String? = null,
    val userId: String? = null,
    val campaignId: String? = null,
    val campaignName: String? = null,

    val strength: Int = 16,
    val dexterity: Int = 14,
    val constitution: Int = 13,
    val intelligence: Int = 10,
    val wisdom: Int = 8,
    val charisma: Int = 12,
    val armorClass: Int = 13,
    val maxHp: Int = 20,
    val currentHp: Int = 20,
    val speed: Int = 30,
    val proficiencyBonus: Int = 2,
    val hitDiceTotal: Int = 3,
    val hitDiceRemaining: Int = 3,
    val hitDieType: Int = 6
)

class CharacterViewModel : ViewModel() {
    private val baseUrl = "http://10.0.2.2:8080"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var currentCharacterData by mutableStateOf<CharacterData?>(null)

    var currentCampaignId by mutableStateOf<String?>(null)
        private set

    val characterName: String
        get() = currentCharacterData?.name ?: "Unknown"
    val characterClass: String
        get() = currentCharacterData?.charClass ?: "Unknown"
    val characterLevel: Int
        get() = currentCharacterData?.level ?: 1

    val proficiencyBonus: Int
        get() = currentCharacterData?.proficiencyBonus ?: 2

    val abilities: List<Ability>
        get() {
            val data = currentCharacterData ?: return defaultAbilities()
            return listOf(
                Ability("Strength", data.strength, isProficient = true),
                Ability("Dexterity", data.dexterity),
                Ability("Constitution", data.constitution, isProficient = true),
                Ability("Intelligence", data.intelligence),
                Ability("Wisdom", data.wisdom),
                Ability("Charisma", data.charisma)
            )
        }

    val skills: List<Skill> by derivedStateOf {
        val skillProficiencies = mapOf(
            "Acrobatics" to true,
            "Athletics" to true,
            "History" to true,
            "Investigation" to true,
            "Perception" to true,
            "Persuasion" to true,
            "Stealth" to true
        )

        listOf(
            Skill("Acrobatics", "Dexterity", true),
            Skill("Animal Handling", "Wisdom", false),
            Skill("Arcana", "Intelligence", false),
            Skill("Athletics", "Strength", true),
            Skill("Deception", "Charisma", false),
            Skill("History", "Intelligence", true),
            Skill("Insight", "Wisdom", false),
            Skill("Intimidation", "Charisma", false),
            Skill("Investigation", "Intelligence", true),
            Skill("Medicine", "Wisdom", false),
            Skill("Nature", "Intelligence", false),
            Skill("Perception", "Wisdom", true),
            Skill("Performance", "Charisma", false),
            Skill("Persuasion", "Charisma", true),
            Skill("Religion", "Intelligence", false),
            Skill("Sleight of Hand", "Dexterity", false),
            Skill("Stealth", "Dexterity", true),
            Skill("Survival", "Wisdom", false)
        ).map { skill ->
            val abilityMod = abilities.find { it.name == skill.abilityName }?.modifier ?: 0
            val profMod = if (skill.isProficient) proficiencyBonus else 0
            skill.copy(modifier = abilityMod + profMod)
        }
    }

    var armorClass by mutableStateOf(13)
    val initiative: Int
        get() = abilities.find { it.name == "Dexterity" }?.modifier ?: 0
    var inspiration by mutableStateOf(false)

    var speed by mutableStateOf(30)

    var currentHP by mutableStateOf(20)
    var maxHP by mutableStateOf(20)

    fun updateMaxHP(newValue: Int) {
        maxHP = newValue
        currentHP = currentHP.coerceAtMost(maxHP)
    }

    var tempHP by mutableStateOf(0)

    val hitDiceTotal: Int
        get() = currentCharacterData?.hitDiceTotal ?: 3
    var remainingHitDice by mutableStateOf(3)

    val hitDieType: Int
        get() = currentCharacterData?.hitDieType ?: 6

    private val _deathSaveSuccesses = mutableStateOf(0)
    val deathSaveSuccesses: State<Int> get() = _deathSaveSuccesses

    private val _deathSaveFailures = mutableStateOf(0)
    val deathSaveFailures: State<Int> get() = _deathSaveFailures

    val passivePerception: Int
        get() {
            val wisdomMod = abilities.find { it.name == "Wisdom" }?.modifier ?: 0
            val isProficient = skills.find { it.name == "Perception" }?.isProficient ?: false
            return 10 + wisdomMod + if (isProficient) proficiencyBonus else 0
        }

    var spellcastingAbilityName by mutableStateOf("Intelligence")

    val spellcastingAbility: Ability?
        get() = abilities.find { it.name == spellcastingAbilityName }

    val spellSaveDC: Int
        get() = 8 + (spellcastingAbility?.modifier ?: 0) + proficiencyBonus

    fun loadCharacterForCampaign(context: Context, campaignId: String) {
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
                    .url("$baseUrl/campaigns/$campaignId/characters/mine")
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val characters = json.decodeFromString<List<CharacterData>>(responseBody)
                        if (characters.isNotEmpty()) {
                            val character = characters.first()
                            updateFromCharacterData(character)
                            currentCampaignId = campaignId
                            Log.d(TAG, "Loaded character: ${character.name}")
                        } else {
                            errorMessage = "No character found for this campaign"
                        }
                    }
                } else {
                    errorMessage = "Failed to load character: ${response.code}"
                    Log.e(TAG, "Failed to load character: ${response.code}")
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
                Log.e(TAG, "Error loading character", e)
            } finally {
                isLoading = false
            }
        }
    }

    private fun updateFromCharacterData(data: CharacterData) {
        currentCharacterData = data
        armorClass = data.armorClass
        speed = data.speed
        currentHP = data.currentHp
        maxHP = data.maxHp
        remainingHitDice = data.hitDiceRemaining
    }

    private fun defaultAbilities() = listOf(
        Ability("Strength", 10),
        Ability("Dexterity", 10),
        Ability("Constitution", 10),
        Ability("Intelligence", 10),
        Ability("Wisdom", 10),
        Ability("Charisma", 10)
    )

    private fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun incrementHitDice() {
        if (remainingHitDice < hitDiceTotal) remainingHitDice++
    }

    fun decrementHitDice() {
        if (remainingHitDice > 0) remainingHitDice--
    }

    fun resetDeathSaves() {
        _deathSaveSuccesses.value = 0
        _deathSaveFailures.value = 0
    }

    fun shortRest() {
        tempHP = 0
        resetDeathSaves()
    }

    fun longRest() {
        currentHP = maxHP
        tempHP = 0
        resetDeathSaves()
    }

    fun applyDeathSaveResult(roll: Int, chatViewModel: ChatViewModel) {
        val message = "/ToDM 🎲 Death Save: Rolled 1d20 = $roll"
        chatViewModel.addMessage(message, MessageType.ROLL, currentCharacterData?.campaignId)

        when {
            roll == 1 -> {
                _deathSaveFailures.value = (_deathSaveFailures.value + 2).coerceAtMost(3)
            }
            roll < 10 -> {
                _deathSaveFailures.value = (_deathSaveFailures.value + 1).coerceAtMost(3)
            }
            roll == 20 -> {
                _deathSaveSuccesses.value = 3
            }
            else -> {
                _deathSaveSuccesses.value = (_deathSaveSuccesses.value + 1).coerceAtMost(3)
            }
        }
    }
}