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

@Serializable
data class ChatMessage(
    val id: String,
    val campaignId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val messageType: MessageType,
    val timestamp: Long,
    val isToGM: Boolean = false
)

@Serializable
enum class MessageType {
    CHAT, ROLL
}

@Serializable
data class SendMessageRequest(
    val campaignId: String,
    val content: String,
    val messageType: MessageType,
    val isToGM: Boolean = false
)

@Serializable
data class GetMessagesResponse(
    val messages: List<ChatMessage>
)

@Serializable
data class Character(
    val id: Int = 0,
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
    val campaignName: String? = null
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
    val backstory: String? = null
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
    val backstory: String? = null
)

@Serializable
data class LevelUpResponse(val updated: Int)

val db = DriverManager.getConnection("jdbc:sqlite:data.db")
val secret = "very-secure-key"

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

        it.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id TEXT PRIMARY KEY,
                campaign_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                content TEXT NOT NULL,
                message_type TEXT NOT NULL,
                is_to_gm INTEGER NOT NULL DEFAULT 0,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
                FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        it.executeUpdate(
            """
            CREATE INDEX IF NOT EXISTS idx_chat_messages_campaign_timestamp 
            ON chat_messages(campaign_id, timestamp);
            """.trimIndent()
        )

        it.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS characters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                char_class TEXT NOT NULL,
                level INTEGER NOT NULL,
                race_name TEXT,
                race_description TEXT,
                class_description TEXT,
                appearance_description TEXT,
                backstory TEXT,
                user_id TEXT,
                campaign_id TEXT,
                created_at INTEGER,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL
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

                get("/campaigns/{campaignId}/messages") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"]
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val before = call.request.queryParameters["before"]?.toLongOrNull()

                    if (campaignId == null) {
                        call.respondText("Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@get
                    }

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to view this campaign's messages", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@get
                    }

                    val isGM = isUserCampaignOwner(userId, campaignId)

                    val messages = mutableListOf<ChatMessage>()

                    val query = if (before != null) {
                        """
                        SELECT id, campaign_id, sender_id, sender_name, content, message_type, is_to_gm, timestamp
                        FROM chat_messages 
                        WHERE campaign_id = ? 
                        AND timestamp < ?
                        AND (is_to_gm = 0 OR sender_id = ? OR ?)
                        ORDER BY timestamp DESC 
                        LIMIT ?
                        """.trimIndent()
                    } else {
                        """
                        SELECT id, campaign_id, sender_id, sender_name, content, message_type, is_to_gm, timestamp
                        FROM chat_messages 
                        WHERE campaign_id = ? 
                        AND (is_to_gm = 0 OR sender_id = ? OR ?)
                        ORDER BY timestamp DESC 
                        LIMIT ?
                        """.trimIndent()
                    }

                    val stmt = db.prepareStatement(query)
                    var paramIndex = 1
                    stmt.setString(paramIndex++, campaignId)
                    if (before != null) {
                        stmt.setLong(paramIndex++, before)
                    }
                    stmt.setString(paramIndex++, userId)
                    stmt.setBoolean(paramIndex++, isGM)
                    stmt.setInt(paramIndex, limit)

                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        messages.add(ChatMessage(
                            id = rs.getString("id"),
                            campaignId = rs.getString("campaign_id"),
                            senderId = rs.getString("sender_id"),
                            senderName = rs.getString("sender_name"),
                            content = rs.getString("content"),
                            messageType = MessageType.valueOf(rs.getString("message_type")),
                            timestamp = rs.getLong("timestamp"),
                            isToGM = rs.getBoolean("is_to_gm")
                        ))
                    }

                    call.respond(GetMessagesResponse(messages.reversed()))
                }

                post("/campaigns/{campaignId}/messages") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val campaignId = call.parameters["campaignId"]
                    val body = call.receive<SendMessageRequest>()

                    if (campaignId == null) {
                        call.respondText("Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (body.campaignId != campaignId) {
                        call.respondText("Campaign ID mismatch", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to send messages to this campaign", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@post
                    }

                    val messageId = UUID.randomUUID().toString()
                    val timestamp = System.currentTimeMillis()

                    val stmt = db.prepareStatement("""
                        INSERT INTO chat_messages (id, campaign_id, sender_id, sender_name, content, message_type, is_to_gm, timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent())

                    stmt.setString(1, messageId)
                    stmt.setString(2, campaignId)
                    stmt.setString(3, userId)
                    stmt.setString(4, username)
                    stmt.setString(5, body.content)
                    stmt.setString(6, body.messageType.name)
                    stmt.setBoolean(7, body.isToGM)
                    stmt.setLong(8, timestamp)

                    stmt.executeUpdate()

                    val message = ChatMessage(
                        id = messageId,
                        campaignId = campaignId,
                        senderId = userId,
                        senderName = username,
                        content = body.content,
                        messageType = body.messageType,
                        timestamp = timestamp,
                        isToGM = body.isToGM
                    )

                    println("Message sent by $username to campaign $campaignId: ${body.content}")
                    call.respond(message)
                }

                delete("/campaigns/{campaignId}/messages/{messageId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"]
                    val messageId = call.parameters["messageId"]

                    if (campaignId == null || messageId == null) {
                        call.respondText("Campaign ID and Message ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@delete
                    }

                    val checkStmt = db.prepareStatement("""
                        SELECT sender_id FROM chat_messages 
                        WHERE id = ? AND campaign_id = ?
                    """.trimIndent())
                    checkStmt.setString(1, messageId)
                    checkStmt.setString(2, campaignId)
                    val rs = checkStmt.executeQuery()

                    if (!rs.next()) {
                        call.respondText("Message not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@delete
                    }

                    val senderId = rs.getString("sender_id")
                    val isGM = isUserCampaignOwner(userId, campaignId)

                    if (senderId != userId && !isGM) {
                        call.respondText("Not authorized to delete this message", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@delete
                    }

                    val deleteStmt = db.prepareStatement("DELETE FROM chat_messages WHERE id = ?")
                    deleteStmt.setString(1, messageId)
                    deleteStmt.executeUpdate()

                    println("Message $messageId deleted by user $userId")
                    call.respondText("Message deleted successfully")
                }

                post("/campaigns/{campaignId}/levelup") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"]

                    if (campaignId == null) {
                        call.respondText("Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@post
                    }

                    // Nur der Owner (DM) darf leveln
                    val check = db.prepareStatement("SELECT 1 FROM campaigns WHERE id = ? AND owner_id = ?")
                    check.setString(1, campaignId)
                    check.setString(2, userId)
                    val rs = check.executeQuery()
                    if (!rs.next()) {
                        call.respondText(
                            "Only the campaign owner can level up characters in this campaign",
                            status = io.ktor.http.HttpStatusCode.Forbidden
                        )
                        return@post
                    }

                    // Alle Charaktere in der Kampagne +1 Level
                    val upd = db.prepareStatement("UPDATE characters SET level = level + 1 WHERE campaign_id = ?")
                    upd.setString(1, campaignId)
                    val updatedRows = upd.executeUpdate() // Anzahl geänderte Zeilen

                    call.respond(LevelUpResponse(updated = updatedRows))
                }

                // Character start
                post("/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val body = call.receive<CreateCharacterRequest>()

                    val stmt = db.prepareStatement("""
        INSERT INTO characters (
            name, char_class, level,
            race_name, race_description,
            class_description, appearance_description, backstory,   -- NEU
            user_id, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """.trimIndent(), java.sql.Statement.RETURN_GENERATED_KEYS)

                    stmt.setString(1, body.name)
                    stmt.setString(2, body.charClass)
                    stmt.setInt(3, body.level)
                    stmt.setString(4, body.raceName)
                    stmt.setString(5, body.raceDescription)
                    stmt.setString(6, body.classDescription)        // NEU
                    stmt.setString(7, body.appearanceDescription)   // NEU
                    stmt.setString(8, body.backstory)               // NEU
                    stmt.setString(9, userId)
                    stmt.setLong(10, System.currentTimeMillis())
                    stmt.executeUpdate()

                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        val characterId = rs.getInt(1)
                        println("Character created: ${body.name} by user $userId")
                        call.respond(
                            Character(
                                id = characterId,
                                name = body.name,
                                charClass = body.charClass,
                                level = body.level,
                                raceName = body.raceName,
                                raceDescription = body.raceDescription,
                                classDescription = body.classDescription,          // NEU
                                appearanceDescription = body.appearanceDescription,// NEU
                                backstory = body.backstory,                        // NEU
                                userId = userId
                            )
                        )
                    } else {
                        call.respondText("Failed to create character", status = io.ktor.http.HttpStatusCode.InternalServerError)
                    }
                }

                get("/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()

                    val characters = mutableListOf<Character>()
                    val stmt = db.prepareStatement("""
        SELECT c.id, c.name, c.char_class, c.level,
               c.race_name, c.race_description,
               c.class_description, c.appearance_description, c.backstory,   -- NEU
               camp.id   AS campaign_id,
               camp.name AS campaign_name
        FROM characters c
        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
        WHERE c.user_id = ?
    """.trimIndent())
                    stmt.setString(1, userId)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        characters.add(
                            Character(
                                id = rs.getInt("id"),
                                name = rs.getString("name"),
                                charClass = rs.getString("char_class"),
                                level = rs.getInt("level"),
                                raceName = rs.getString("race_name"),
                                raceDescription = rs.getString("race_description"),
                                classDescription = rs.getString("class_description"),           // NEU
                                appearanceDescription = rs.getString("appearance_description"), // NEU
                                backstory = rs.getString("backstory"),                          // NEU
                                userId = userId,
                                campaignId = rs.getString("campaign_id"),
                                campaignName = rs.getString("campaign_name")
                            )
                        )
                    }

                    call.respond(characters)
                }

                put("/characters/{characterId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()

                    if (characterId == null) {
                        call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@put
                    }

                    // gehört der Character dem User?
                    val checkStmt = db.prepareStatement("SELECT user_id FROM characters WHERE id = ?")
                    checkStmt.setInt(1, characterId)
                    val rsCheck = checkStmt.executeQuery()
                    if (!rsCheck.next()) {
                        call.respondText("Character not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@put
                    }
                    if (rsCheck.getString("user_id") != userId) {
                        call.respondText("Not authorized", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@put
                    }

                    val body = call.receive<UpdateCharacterRequest>()

                    val upd = db.prepareStatement(
                        """
        UPDATE characters
        SET name = ?, 
            char_class = ?, 
            level = ?, 
            race_name = ?, 
            race_description = ?, 
            class_description = ?, 
            appearance_description = ?, 
            backstory = ?
        WHERE id = ? AND user_id = ?
        """.trimIndent()
                    )
                    upd.setString(1, body.name)
                    upd.setString(2, body.charClass)
                    upd.setInt(3, body.level)
                    upd.setString(4, body.raceName)
                    upd.setString(5, body.raceDescription)
                    upd.setString(6, body.classDescription)
                    upd.setString(7, body.appearanceDescription)
                    upd.setString(8, body.backstory)
                    upd.setInt(9, characterId)
                    upd.setString(10, userId)

                    // aktualisierten Datensatz zurückgeben (inkl. Kampagneninfo)
                    val sel = db.prepareStatement(
                        """
        SELECT c.id, c.name, c.char_class, c.level,
               c.race_name, c.race_description,
               c.class_description, c.appearance_description, c.backstory,
               camp.id   AS campaign_id,
               camp.name AS campaign_name
        FROM characters c
        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
        WHERE c.id = ?
        """.trimIndent()
                    )
                    sel.setInt(1, characterId)
                    val rs = sel.executeQuery()
                    rs.next()

                    call.respond(
                        Character(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            charClass = rs.getString("char_class"),
                            level = rs.getInt("level"),
                            raceName = rs.getString("race_name"),
                            raceDescription = rs.getString("race_description"),
                            classDescription = rs.getString("class_description"),
                            appearanceDescription = rs.getString("appearance_description"),
                            backstory = rs.getString("backstory"),
                            userId = userId,
                            campaignId = rs.getString("campaign_id"),
                            campaignName = rs.getString("campaign_name")
                        )
                    )
                }




                // Character löschen
                delete("/characters/{characterId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()

                    if (characterId == null) {
                        call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@delete
                    }

                    // Prüfe ob der Charakter dem Benutzer gehört
                    val checkStmt = db.prepareStatement("SELECT user_id, campaign_id FROM characters WHERE id = ?")
                    checkStmt.setInt(1, characterId)
                    val rs = checkStmt.executeQuery()

                    if (!rs.next()) {
                        call.respondText("Character not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@delete
                    }

                    val characterUserId = rs.getString("user_id")
                    if (characterUserId != userId) {
                        call.respondText("Not authorized to delete this character", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@delete
                    }

                    // Prüfe ob der Charakter einer Kampagne zugewiesen ist
                    val campaignId = rs.getString("campaign_id")
                    if (campaignId != null) {
                        call.respondText("Cannot delete character assigned to a campaign. Please remove from campaign first.", status = io.ktor.http.HttpStatusCode.Conflict)
                        return@delete
                    }

                    // Lösche den Charakter
                    val deleteStmt = db.prepareStatement("DELETE FROM characters WHERE id = ?")
                    deleteStmt.setInt(1, characterId)
                    val rowsDeleted = deleteStmt.executeUpdate()

                    if (rowsDeleted > 0) {
                        println("Character $characterId deleted by user $userId")
                        call.respondText("Character deleted successfully")
                    } else {
                        call.respondText("Failed to delete character", status = io.ktor.http.HttpStatusCode.InternalServerError)
                    }
                }

                get("/campaigns/{campaignId}/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"]

                    if (campaignId == null) {
                        call.respondText("Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@get
                    }

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to view this campaign's characters", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@get
                    }

                    val characters = mutableListOf<Character>()
                    val stmt = db.prepareStatement("""
                        SELECT c.id, c.name, c.char_class, c.level, c.race_name, c.race_description, u.username as owner_name
                        FROM characters c
                        JOIN users u ON c.user_id = u.id
                        WHERE c.campaign_id = ?
                    """.trimIndent())

                    stmt.setString(1, campaignId)
                    val rs = stmt.executeQuery()

                    while (rs.next()) {
                        characters.add(Character(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            charClass = rs.getString("char_class"),
                            level = rs.getInt("level"),
                            raceName = rs.getString("race_name"),
                            raceDescription = rs.getString("race_description")
                        ))
                    }

                    call.respond(characters)
                }

                put("/characters/{characterId}/assign/{campaignId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]
                    val campaignId = call.parameters["campaignId"]

                    if (characterId == null || campaignId == null) {
                        call.respondText("Character ID and Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@put
                    }

                    // Check if user owns the character
                    val checkStmt = db.prepareStatement("SELECT 1 FROM characters WHERE id = ? AND user_id = ?")
                    checkStmt.setInt(1, characterId.toInt())
                    checkStmt.setString(2, userId)
                    val rs = checkStmt.executeQuery()

                    if (!rs.next()) {
                        call.respondText("Character not found or not owned by user", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@put
                    }

                    // Check if user is in the campaign
                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to assign character to this campaign", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@put
                    }

                    val updateStmt = db.prepareStatement("UPDATE characters SET campaign_id = ? WHERE id = ?")
                    updateStmt.setString(1, campaignId)
                    updateStmt.setInt(2, characterId.toInt())
                    updateStmt.executeUpdate()

                    call.respondText("Character assigned to campaign successfully")
                }
                put("/characters/{characterId}/unassign") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()

                    if (characterId == null) {
                        call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@put
                    }

                    // gehört der Character dem User?
                    val checkStmt = db.prepareStatement("SELECT user_id FROM characters WHERE id = ?")
                    checkStmt.setInt(1, characterId)
                    val rs = checkStmt.executeQuery()
                    if (!rs.next()) {
                        call.respondText("Character not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@put
                    }
                    if (rs.getString("user_id") != userId) {
                        call.respondText("Not authorized", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@put
                    }

                    val updateStmt = db.prepareStatement("UPDATE characters SET campaign_id = NULL WHERE id = ?")
                    updateStmt.setInt(1, characterId)
                    updateStmt.executeUpdate()

                    call.respondText("Character unassigned from campaign")
                }//ende character

            }
        }
    }.start(wait = true)
}

fun isUserInCampaign(userId: String, campaignId: String): Boolean {
    val stmt = db.prepareStatement("SELECT 1 FROM campaign_players WHERE player_id = ? AND campaign_id = ?")
    stmt.setString(1, userId)
    stmt.setString(2, campaignId)
    val rs = stmt.executeQuery()
    return rs.next()
}

fun isUserCampaignOwner(userId: String, campaignId: String): Boolean {
    val stmt = db.prepareStatement("SELECT 1 FROM campaigns WHERE owner_id = ? AND id = ?")
    stmt.setString(1, userId)
    stmt.setString(2, campaignId)
    val rs = stmt.executeQuery()
    return rs.next()
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