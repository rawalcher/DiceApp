package com.example.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import org.mindrot.jbcrypt.BCrypt
import java.sql.DriverManager
import java.util.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

@Serializable
data class UserCredentials(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String)

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

val db = DriverManager.getConnection("jdbc:sqlite:data.db")
val secret = "very-secure-key"

// TODO Maybe split Server into subclasses

fun initializeTables() {
    db.createStatement().use {
        it.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL
            );
            """.trimIndent()
        )

        it.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS campaigns (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                owner_id TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                max_players INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        it.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS campaign_players (
                campaign_id TEXT,
                player_id TEXT,
                player_name TEXT,
                joined_at INTEGER NOT NULL,
                PRIMARY KEY (campaign_id, player_id)
            );
            """.trimIndent()
        )
    }
}

fun main() {
    initializeTables()

    println("Server started on http://localhost:8080")

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        install(Authentication) {
            jwt {
                realm = "dnd-app"
                verifier(
                    JWT
                        .require(Algorithm.HMAC256(secret))
                        .withIssuer("dnd-server")
                        .build()
                )
                validate { credential ->
                    val userId = credential.payload.getClaim("userId").asString()
                    if (userId != null) JWTPrincipal(credential.payload) else null
                }
            }
        }

        routing {
            post("/register") {
                val body = call.receive<UserCredentials>()
                val id = UUID.randomUUID().toString()
                val hash = BCrypt.hashpw(body.password, BCrypt.gensalt())

                try {
                    val stmt = db.prepareStatement("INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)")
                    stmt.setString(1, id)
                    stmt.setString(2, body.username)
                    stmt.setString(3, hash)
                    stmt.executeUpdate()
                    println("New user registered: ${body.username} (id=$id)")
                    val token = generateToken(id, body.username)
                    call.respond(TokenResponse(token))
                } catch (_: Exception) {
                    println("Registration failed: username '${body.username}' already exists")
                    call.respondText("Username already exists", status = io.ktor.http.HttpStatusCode.Conflict)
                }
            }

            post("/login") {
                val body = call.receive<UserCredentials>()
                val stmt = db.prepareStatement("SELECT id, password_hash FROM users WHERE username = ?")
                stmt.setString(1, body.username)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val userId = rs.getString("id")
                    val hash = rs.getString("password_hash")
                    if (BCrypt.checkpw(body.password, hash)) {
                        println("User logged in: ${body.username} (id=$userId)")
                        val token = generateToken(userId, body.username)
                        call.respond(TokenResponse(token))
                        return@post
                    }
                }
                println("Failed login attempt for username: ${body.username}")
                call.respondText("Invalid credentials", status = io.ktor.http.HttpStatusCode.Unauthorized)
            }

            authenticate {
                get("/me") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    println("Authenticated user: $username (id=$userId)")
                    call.respondText("You are logged in as user '$username' with ID: $userId")
                }

                // TODO Split SQL into Methods to reduce bloat and make comments unnecessary

                // Get all campaigns with join status for current user
                get("/campaigns") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()

                    val campaigns = mutableListOf<Campaign>()

                    val stmt = db.prepareStatement("""
                        SELECT c.id, c.name, c.description, c.owner_id, c.owner_name, c.max_players,
                               COUNT(cp.player_id) as player_count,
                               CASE WHEN cp_user.player_id IS NOT NULL THEN 1 ELSE 0 END as is_joined
                        FROM campaigns c
                        LEFT JOIN campaign_players cp ON c.id = cp.campaign_id
                        LEFT JOIN campaign_players cp_user ON c.id = cp_user.campaign_id AND cp_user.player_id = ?
                        GROUP BY c.id, c.name, c.description, c.owner_id, c.owner_name, c.max_players, cp_user.player_id
                        ORDER BY c.created_at DESC
                    """.trimIndent())

                    stmt.setString(1, userId)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        campaigns.add(Campaign(
                            id = rs.getString("id"),
                            name = rs.getString("name"),
                            description = rs.getString("description"),
                            ownerId = rs.getString("owner_id"),
                            ownerName = rs.getString("owner_name"),
                            playerCount = rs.getInt("player_count"),
                            maxPlayers = rs.getInt("max_players"),
                            isJoined = rs.getInt("is_joined") == 1
                        ))
                    }

                    call.respond(campaigns)
                }

                // Create a new campaign
                post("/campaigns") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val body = call.receive<CreateCampaignRequest>()

                    val campaignId = UUID.randomUUID().toString()

                    val stmt = db.prepareStatement("""
                        INSERT INTO campaigns (id, name, description, owner_id, owner_name, max_players, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent())

                    stmt.setString(1, campaignId)
                    stmt.setString(2, body.name)
                    stmt.setString(3, body.description)
                    stmt.setString(4, userId)
                    stmt.setString(5, username)
                    stmt.setInt(6, body.maxPlayers)
                    stmt.setLong(7, System.currentTimeMillis())

                    stmt.executeUpdate()

                    // Auto-join creator to the campaign
                    val joinStmt = db.prepareStatement("""
                        INSERT INTO campaign_players (campaign_id, player_id, player_name, joined_at)
                        VALUES (?, ?, ?, ?)
                    """.trimIndent())

                    joinStmt.setString(1, campaignId)
                    joinStmt.setString(2, userId)
                    joinStmt.setString(3, username)
                    joinStmt.setLong(4, System.currentTimeMillis())
                    joinStmt.executeUpdate()

                    println("Campaign created: ${body.name} by $username (id=$campaignId)")
                    call.respondText("Campaign created successfully")
                }

                post("/campaigns/join") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val body = call.receive<JoinCampaignRequest>()

                    // Check if campaign exists and has space
                    val checkStmt = db.prepareStatement("""
                        SELECT c.max_players, COUNT(cp.player_id) as current_players
                        FROM campaigns c
                        LEFT JOIN campaign_players cp ON c.id = cp.campaign_id
                        WHERE c.id = ?
                        GROUP BY c.id, c.max_players
                    """.trimIndent())

                    checkStmt.setString(1, body.campaignId)
                    val rs = checkStmt.executeQuery()

                    if (!rs.next()) {
                        call.respondText("Campaign not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@post
                    }

                    val maxPlayers = rs.getInt("max_players")
                    val currentPlayers = rs.getInt("current_players")

                    if (currentPlayers >= maxPlayers) {
                        call.respondText("Campaign is full", status = io.ktor.http.HttpStatusCode.Conflict)
                        return@post
                    }

                    try {
                        val stmt = db.prepareStatement("""
                            INSERT INTO campaign_players (campaign_id, player_id, player_name, joined_at)
                            VALUES (?, ?, ?, ?)
                        """.trimIndent())

                        stmt.setString(1, body.campaignId)
                        stmt.setString(2, userId)
                        stmt.setString(3, username)
                        stmt.setLong(4, System.currentTimeMillis())
                        stmt.executeUpdate()

                        println("User $username joined campaign ${body.campaignId}")
                        call.respondText("Joined campaign successfully")
                    } catch (_: Exception) {
                        call.respondText("Already joined this campaign", status = io.ktor.http.HttpStatusCode.Conflict)
                    }
                }

                // Delete a campaign (only owner can delete)
                delete("/campaigns/{campaignId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"]

                    if (campaignId == null) {
                        call.respondText("Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@delete
                    }

                    // Check if user is the owner
                    val checkStmt = db.prepareStatement("SELECT owner_id FROM campaigns WHERE id = ?")
                    checkStmt.setString(1, campaignId)
                    val rs = checkStmt.executeQuery()

                    if (!rs.next()) {
                        call.respondText("Campaign not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@delete
                    }

                    val ownerId = rs.getString("owner_id")
                    if (ownerId != userId) {
                        call.respondText("Only the campaign owner can delete this campaign", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@delete
                    }

                    // Delete campaign players first
                    val deletePlayersStmt = db.prepareStatement("DELETE FROM campaign_players WHERE campaign_id = ?")
                    deletePlayersStmt.setString(1, campaignId)
                    deletePlayersStmt.executeUpdate()

                    // Delete campaign
                    val deleteCampaignStmt = db.prepareStatement("DELETE FROM campaigns WHERE id = ?")
                    deleteCampaignStmt.setString(1, campaignId)
                    deleteCampaignStmt.executeUpdate()

                    println("Campaign $campaignId deleted by user $userId")
                    call.respondText("Campaign deleted successfully")
                }
            }
        }
    }.start(wait = true)
}

fun generateToken(userId: String, username: String): String {
    return JWT.create()
        .withAudience("dnd-app")
        .withIssuer("dnd-server")
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 1000 * 60 * 60))
        .sign(Algorithm.HMAC256(secret))
}