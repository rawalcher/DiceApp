package com.example.diceapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Campaign(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val ownerName: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val isJoined: Boolean = false
)

@Serializable
data class CreateCampaignRequest(
    val name: String,
    val description: String,
    val maxPlayers: Int
)

@Serializable
data class JoinCampaignRequest(
    val campaignId: String
)