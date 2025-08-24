package com.example.diceapp.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class CampaignSelection {
    var id by mutableStateOf<String?>(null)
}

val LocalCampaignSelection = staticCompositionLocalOf<CampaignSelection> {
    error("LocalCampaignSelection not provided")
}
