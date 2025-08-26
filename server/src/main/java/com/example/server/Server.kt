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
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

// =============================
//  Data Models (Serializable)
// =============================
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
enum class MessageType { CHAT, ROLL }

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
    val campaignName: String? = null,
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

@Serializable
data class LevelUpResponse(val updated: Int)

// =============================
//  DB & Auth
// =============================
val db = DriverManager.getConnection("jdbc:sqlite:data.db")
val secret = "very-secure-key"

fun initializeTables() {
    db.createStatement().use { st ->
        // Wichtig für SQLite: FK erzwingen
        st.execute("PRAGMA foreign_keys = ON")

        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL
            );
            """.trimIndent()
        )

        st.executeUpdate(
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

        st.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS campaign_players (
                campaign_id TEXT,
                player_id TEXT,
                player_name TEXT,
                joined_at INTEGER NOT NULL,
                PRIMARY KEY (campaign_id, player_id),
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
                FOREIGN KEY (player_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        st.executeUpdate(
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

        st.executeUpdate(
            """
            CREATE INDEX IF NOT EXISTS idx_chat_messages_campaign_timestamp 
            ON chat_messages(campaign_id, timestamp);
            """.trimIndent()
        )

        st.executeUpdate(
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
                strength INTEGER NOT NULL DEFAULT 10,
                dexterity INTEGER NOT NULL DEFAULT 10,
                constitution INTEGER NOT NULL DEFAULT 10,
                intelligence INTEGER NOT NULL DEFAULT 10,
                wisdom INTEGER NOT NULL DEFAULT 10,
                charisma INTEGER NOT NULL DEFAULT 10,
                armor_class INTEGER NOT NULL DEFAULT 10,
                max_hp INTEGER NOT NULL DEFAULT 10,
                current_hp INTEGER NOT NULL DEFAULT 10,
                speed INTEGER NOT NULL DEFAULT 30,
                proficiency_bonus INTEGER NOT NULL DEFAULT 2,
                hit_dice_total INTEGER NOT NULL DEFAULT 1,
                hit_dice_remaining INTEGER NOT NULL DEFAULT 1,
                hit_die_type INTEGER NOT NULL DEFAULT 6,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL
            );
            """.trimIndent()
        )

        // sinnvolle Indizes
        st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_characters_user ON characters(user_id)")
        st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_characters_campaign ON characters(campaign_id)")

        try {
            st.executeUpdate(
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_user_campaign_single_character " +
                        "ON characters(user_id, campaign_id) WHERE campaign_id IS NOT NULL"
            )
        } catch (e: Exception) {
            println("Warnung: Unique-Index nicht erstellt (evtl. Duplikate vorhanden): ${e.message}")
        }
    }
}

fun migrateCharactersTable() {
    val cols = listOf(
        "strength INTEGER NOT NULL DEFAULT 10",
        "dexterity INTEGER NOT NULL DEFAULT 10",
        "constitution INTEGER NOT NULL DEFAULT 10",
        "intelligence INTEGER NOT NULL DEFAULT 10",
        "wisdom INTEGER NOT NULL DEFAULT 10",
        "charisma INTEGER NOT NULL DEFAULT 10",
        "armor_class INTEGER NOT NULL DEFAULT 10",
        "max_hp INTEGER NOT NULL DEFAULT 10",
        "current_hp INTEGER NOT NULL DEFAULT 10",
        "speed INTEGER NOT NULL DEFAULT 30",
        "proficiency_bonus INTEGER NOT NULL DEFAULT 2",
        "hit_dice_total INTEGER NOT NULL DEFAULT 1",
        "hit_dice_remaining INTEGER NOT NULL DEFAULT 1",
        "hit_die_type INTEGER NOT NULL DEFAULT 6"
    )
    cols.forEach { def ->
        try { db.createStatement().use { it.executeUpdate("ALTER TABLE characters ADD COLUMN $def") } }
        catch (_: Exception) { /* Spalte existiert bereits */ }
    }
}

// =============================
//  Helpers
// =============================
private fun mapCharacter(rs: ResultSet, userIdFallback: String? = null): Character = Character(
    id = rs.getInt("id"),
    name = rs.getString("name"),
    charClass = rs.getString("char_class"),
    level = rs.getInt("level"),
    raceName = rs.getString("race_name"),
    raceDescription = rs.getString("race_description"),
    classDescription = rs.getString("class_description"),
    appearanceDescription = rs.getString("appearance_description"),
    backstory = rs.getString("backstory"),
    userId = userIdFallback, // meist nicht selektiert – optional setzen
    campaignId = rs.getString("campaign_id"),
    campaignName = rs.getString("campaign_name"),
    strength = rs.getInt("strength"),
    dexterity = rs.getInt("dexterity"),
    constitution = rs.getInt("constitution"),
    intelligence = rs.getInt("intelligence"),
    wisdom = rs.getInt("wisdom"),
    charisma = rs.getInt("charisma"),
    armorClass = rs.getInt("armor_class"),
    maxHp = rs.getInt("max_hp"),
    currentHp = rs.getInt("current_hp"),
    speed = rs.getInt("speed"),
    proficiencyBonus = rs.getInt("proficiency_bonus"),
    hitDiceTotal = rs.getInt("hit_dice_total"),
    hitDiceRemaining = rs.getInt("hit_dice_remaining"),
    hitDieType = rs.getInt("hit_die_type")
)

private fun selectCharacterById(id: Int): Character? {
    val sql = """
        SELECT c.id, c.name, c.char_class, c.level,
               c.race_name, c.race_description,
               c.class_description, c.appearance_description, c.backstory,
               c.campaign_id, camp.name AS campaign_name,
               c.strength, c.dexterity, c.constitution, c.intelligence, c.wisdom, c.charisma,
               c.armor_class, c.max_hp, c.current_hp, c.speed, c.proficiency_bonus,
               c.hit_dice_total, c.hit_dice_remaining, c.hit_die_type
        FROM characters c
        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
        WHERE c.id = ?
    """.trimIndent()
    db.prepareStatement(sql).use { ps ->
        ps.setInt(1, id)
        ps.executeQuery().use { rs ->
            return if (rs.next()) mapCharacter(rs) else null
        }
    }
}

fun isUserInCampaign(userId: String, campaignId: String): Boolean {
    db.prepareStatement("SELECT 1 FROM campaign_players WHERE player_id = ? AND campaign_id = ?").use { ps ->
        ps.setString(1, userId)
        ps.setString(2, campaignId)
        ps.executeQuery().use { rs -> return rs.next() }
    }
}

fun isUserCampaignOwner(userId: String, campaignId: String): Boolean {
    db.prepareStatement("SELECT 1 FROM campaigns WHERE owner_id = ? AND id = ?").use { ps ->
        ps.setString(1, userId)
        ps.setString(2, campaignId)
        ps.executeQuery().use { rs -> return rs.next() }
    }
}

fun generateToken(userId: String, username: String): String =
    JWT.create()
        .withAudience("dnd-app")
        .withIssuer("dnd-server")
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1h
        .sign(Algorithm.HMAC256(secret))

fun userHasCharacterInCampaign(
    userId: String,
    campaignId: String,
    exceptCharacterId: Int? = null
): Boolean {
    val sql = if (exceptCharacterId != null) {
        "SELECT 1 FROM characters WHERE user_id = ? AND campaign_id = ? AND id <> ? LIMIT 1"
    } else {
        "SELECT 1 FROM characters WHERE user_id = ? AND campaign_id = ? LIMIT 1"
    }
    db.prepareStatement(sql).use { ps ->
        ps.setString(1, userId)
        ps.setString(2, campaignId)
        if (exceptCharacterId != null) ps.setInt(3, exceptCharacterId)
        ps.executeQuery().use { rs -> return rs.next() }
    }
}

// =============================
//  Server
// =============================
fun main() {
    initializeTables()
    migrateCharactersTable()

    println("Server started on http://localhost:8080")

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
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
                    db.prepareStatement("INSERT INTO users (id, username, password_hash) VALUES (?, ?, ?)").use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, body.username)
                        ps.setString(3, hash)
                        ps.executeUpdate()
                    }
                    println("New user registered: ${body.username} (id=$id)")
                    call.respond(TokenResponse(generateToken(id, body.username)))
                } catch (_: Exception) {
                    println("Registration failed: username '${body.username}' already exists")
                    call.respondText("Username already exists", status = io.ktor.http.HttpStatusCode.Conflict)
                }
            }

            post("/login") {
                val body = call.receive<UserCredentials>()
                db.prepareStatement("SELECT id, password_hash FROM users WHERE username = ?").use { ps ->
                    ps.setString(1, body.username)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val userId = rs.getString("id")
                            val hash = rs.getString("password_hash")
                            if (BCrypt.checkpw(body.password, hash)) {
                                println("User logged in: ${body.username} (id=$userId)")
                                call.respond(TokenResponse(generateToken(userId, body.username)))
                                return@post
                            }
                        }
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

                // ---- Campaigns ----
                get("/campaigns") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()

                    val campaigns = mutableListOf<Campaign>()
                    val sql = """
                        SELECT c.id, c.name, c.description, c.owner_id, c.owner_name, c.max_players,
                               COUNT(cp.player_id) as player_count,
                               CASE WHEN cp_user.player_id IS NOT NULL THEN 1 ELSE 0 END as is_joined
                        FROM campaigns c
                        LEFT JOIN campaign_players cp ON c.id = cp.campaign_id
                        LEFT JOIN campaign_players cp_user ON c.id = cp_user.campaign_id AND cp_user.player_id = ?
                        GROUP BY c.id, c.name, c.description, c.owner_id, c.owner_name, c.max_players, cp_user.player_id
                        ORDER BY c.created_at DESC
                    """.trimIndent()

                    db.prepareStatement(sql).use { ps ->
                        ps.setString(1, userId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                campaigns.add(
                                    Campaign(
                                        id = rs.getString("id"),
                                        name = rs.getString("name"),
                                        description = rs.getString("description"),
                                        ownerId = rs.getString("owner_id"),
                                        ownerName = rs.getString("owner_name"),
                                        playerCount = rs.getInt("player_count"),
                                        maxPlayers = rs.getInt("max_players"),
                                        isJoined = rs.getInt("is_joined") == 1
                                    )
                                )
                            }
                        }
                    }
                    call.respond(campaigns)
                }

                post("/campaigns") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val body = call.receive<CreateCampaignRequest>()

                    val campaignId = UUID.randomUUID().toString()

                    db.prepareStatement(
                        """
                        INSERT INTO campaigns (id, name, description, owner_id, owner_name, max_players, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                    ).use { ps ->
                        ps.setString(1, campaignId)
                        ps.setString(2, body.name)
                        ps.setString(3, body.description)
                        ps.setString(4, userId)
                        ps.setString(5, username)
                        ps.setInt(6, body.maxPlayers)
                        ps.setLong(7, System.currentTimeMillis())
                        ps.executeUpdate()
                    }

                    // Auto-join creator
                    db.prepareStatement(
                        """
                        INSERT INTO campaign_players (campaign_id, player_id, player_name, joined_at)
                        VALUES (?, ?, ?, ?)
                        """.trimIndent()
                    ).use { ps ->
                        ps.setString(1, campaignId)
                        ps.setString(2, userId)
                        ps.setString(3, username)
                        ps.setLong(4, System.currentTimeMillis())
                        ps.executeUpdate()
                    }

                    println("Campaign created: ${body.name} by $username (id=$campaignId)")
                    call.respondText("Campaign created successfully")
                }

                post("/campaigns/join") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val body = call.receive<JoinCampaignRequest>()

                    val checkSql = """
                        SELECT c.max_players, COUNT(cp.player_id) as current_players
                        FROM campaigns c
                        LEFT JOIN campaign_players cp ON c.id = cp.campaign_id
                        WHERE c.id = ?
                        GROUP BY c.id, c.max_players
                    """.trimIndent()
                    db.prepareStatement(checkSql).use { ps ->
                        ps.setString(1, body.campaignId)
                        ps.executeQuery().use { rs ->
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
                        }
                    }

                    try {
                        db.prepareStatement(
                            """
                            INSERT INTO campaign_players (campaign_id, player_id, player_name, joined_at)
                            VALUES (?, ?, ?, ?)
                            """.trimIndent()
                        ).use { ps ->
                            ps.setString(1, body.campaignId)
                            ps.setString(2, userId)
                            ps.setString(3, username)
                            ps.setLong(4, System.currentTimeMillis())
                            ps.executeUpdate()
                        }
                        println("User $username joined campaign ${body.campaignId}")
                        call.respondText("Joined campaign successfully")
                    } catch (_: Exception) {
                        call.respondText("Already joined this campaign", status = io.ktor.http.HttpStatusCode.Conflict)
                    }
                }

                delete("/campaigns/{campaignId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@delete call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )

                    db.prepareStatement("SELECT owner_id FROM campaigns WHERE id = ?").use { ps ->
                        ps.setString(1, campaignId)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) {
                                call.respondText("Campaign not found", status = io.ktor.http.HttpStatusCode.NotFound)
                                return@delete
                            }
                            if (rs.getString("owner_id") != userId) {
                                call.respondText("Only the campaign owner can delete this campaign", status = io.ktor.http.HttpStatusCode.Forbidden)
                                return@delete
                            }
                        }
                    }

                    db.prepareStatement("DELETE FROM campaign_players WHERE campaign_id = ?").use { ps ->
                        ps.setString(1, campaignId)
                        ps.executeUpdate()
                    }
                    db.prepareStatement("DELETE FROM campaigns WHERE id = ?").use { ps ->
                        ps.setString(1, campaignId)
                        ps.executeUpdate()
                    }
                    println("Campaign $campaignId deleted by user $userId")
                    call.respondText("Campaign deleted successfully")
                }

                // ---- Chat ----
                get("/campaigns/{campaignId}/messages") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@get call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val before = call.request.queryParameters["before"]?.toLongOrNull()

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

                    db.prepareStatement(query).use { ps ->
                        var i = 1
                        ps.setString(i++, campaignId)
                        if (before != null) ps.setLong(i++, before)
                        ps.setString(i++, userId)
                        ps.setBoolean(i++, isGM)
                        ps.setInt(i, limit)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) {
                                messages.add(
                                    ChatMessage(
                                        id = rs.getString("id"),
                                        campaignId = rs.getString("campaign_id"),
                                        senderId = rs.getString("sender_id"),
                                        senderName = rs.getString("sender_name"),
                                        content = rs.getString("content"),
                                        messageType = MessageType.valueOf(rs.getString("message_type")),
                                        timestamp = rs.getLong("timestamp"),
                                        isToGM = rs.getBoolean("is_to_gm")
                                    )
                                )
                            }
                        }
                    }

                    call.respond(GetMessagesResponse(messages.reversed()))
                }

                post("/campaigns/{campaignId}/messages") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val username = principal.payload.getClaim("username").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@post call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    val body = call.receive<SendMessageRequest>()

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

                    db.prepareStatement(
                        """
                        INSERT INTO chat_messages (id, campaign_id, sender_id, sender_name, content, message_type, is_to_gm, timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent()
                    ).use { ps ->
                        ps.setString(1, messageId)
                        ps.setString(2, campaignId)
                        ps.setString(3, userId)
                        ps.setString(4, username)
                        ps.setString(5, body.content)
                        ps.setString(6, body.messageType.name)
                        ps.setBoolean(7, body.isToGM)
                        ps.setLong(8, timestamp)
                        ps.executeUpdate()
                    }

                    call.respond(
                        ChatMessage(
                            id = messageId,
                            campaignId = campaignId,
                            senderId = userId,
                            senderName = username,
                            content = body.content,
                            messageType = body.messageType,
                            timestamp = timestamp,
                            isToGM = body.isToGM
                        )
                    )
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

                    val senderId = db.prepareStatement(
                        "SELECT sender_id FROM chat_messages WHERE id = ? AND campaign_id = ?"
                    ).use { ps ->
                        ps.setString(1, messageId)
                        ps.setString(2, campaignId)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getString("sender_id") else null }
                    }

                    if (senderId == null) {
                        call.respondText("Message not found", status = io.ktor.http.HttpStatusCode.NotFound)
                        return@delete
                    }

                    val isGM = isUserCampaignOwner(userId, campaignId)
                    if (senderId != userId && !isGM) {
                        call.respondText("Not authorized to delete this message", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@delete
                    }

                    db.prepareStatement("DELETE FROM chat_messages WHERE id = ?").use { ps ->
                        ps.setString(1, messageId)
                        ps.executeUpdate()
                    }
                    call.respondText("Message deleted successfully")
                }

                post("/campaigns/{campaignId}/levelup") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@post call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )

                    if (!isUserCampaignOwner(userId, campaignId)) {
                        call.respondText(
                            "Only the campaign owner can level up characters in this campaign",
                            status = io.ktor.http.HttpStatusCode.Forbidden
                        )
                        return@post
                    }

                    db.prepareStatement("UPDATE characters SET level = level + 1 WHERE campaign_id = ?").use { ps ->
                        ps.setString(1, campaignId)
                        val updatedRows = ps.executeUpdate()
                        call.respond(LevelUpResponse(updated = updatedRows))
                    }
                }

                // ---- Characters ----
                post("/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val body = call.receive<CreateCharacterRequest>()

                    val sql = """
                        INSERT INTO characters (
                            name, char_class, level,
                            race_name, race_description,
                            class_description, appearance_description, backstory,
                            user_id, created_at,
                            strength, dexterity, constitution, intelligence, wisdom, charisma,
                            armor_class, max_hp, current_hp, speed, proficiency_bonus,
                            hit_dice_total, hit_dice_remaining, hit_die_type
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()

                    val newId: Int
                    db.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { ps ->
                        var i = 1
                        ps.setString(i++, body.name)
                        ps.setString(i++, body.charClass)
                        ps.setInt(i++, body.level)
                        ps.setString(i++, body.raceName)
                        ps.setString(i++, body.raceDescription)
                        ps.setString(i++, body.classDescription)
                        ps.setString(i++, body.appearanceDescription)
                        ps.setString(i++, body.backstory)
                        ps.setString(i++, userId)
                        ps.setLong(i++, System.currentTimeMillis())
                        ps.setInt(i++, body.strength)
                        ps.setInt(i++, body.dexterity)
                        ps.setInt(i++, body.constitution)
                        ps.setInt(i++, body.intelligence)
                        ps.setInt(i++, body.wisdom)
                        ps.setInt(i++, body.charisma)
                        ps.setInt(i++, body.armorClass)
                        ps.setInt(i++, body.maxHp)
                        ps.setInt(i++, body.currentHp)
                        ps.setInt(i++, body.speed)
                        ps.setInt(i++, body.proficiencyBonus)
                        ps.setInt(i++, body.hitDiceTotal)
                        ps.setInt(i++, body.hitDiceRemaining)
                        ps.setInt(i, body.hitDieType)
                        ps.executeUpdate()

                        ps.generatedKeys.use { rs ->
                            if (!rs.next()) {
                                call.respondText("Failed to create character", status = io.ktor.http.HttpStatusCode.InternalServerError)
                                return@post
                            }
                            newId = rs.getInt(1)
                        }
                    }

                    val created = selectCharacterById(newId)!!
                    call.respond(created.copy(userId = userId))
                }

                get("/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()

                    val characters = mutableListOf<Character>()
                    val sql = """
                        SELECT c.id, c.name, c.char_class, c.level,
                               c.race_name, c.race_description,
                               c.class_description, c.appearance_description, c.backstory,
                               c.campaign_id, camp.name AS campaign_name,
                               c.strength, c.dexterity, c.constitution, c.intelligence, c.wisdom, c.charisma,
                               c.armor_class, c.max_hp, c.current_hp, c.speed, c.proficiency_bonus,
                               c.hit_dice_total, c.hit_dice_remaining, c.hit_die_type
                        FROM characters c
                        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
                        WHERE c.user_id = ?
                        ORDER BY c.created_at DESC
                    """.trimIndent()

                    db.prepareStatement(sql).use { ps ->
                        ps.setString(1, userId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) characters.add(mapCharacter(rs, userId))
                        }
                    }
                    call.respond(characters)
                }

                put("/characters/{characterId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()
                        ?: return@put call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)


                    db.prepareStatement("SELECT user_id FROM characters WHERE id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) return@put call.respondText("Character not found", status = io.ktor.http.HttpStatusCode.NotFound)
                            if (rs.getString("user_id") != userId) return@put call.respondText("Not authorized", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }
                    }

                    val body = call.receive<UpdateCharacterRequest>()

                    val sql = """
                        UPDATE characters SET
                            name=?, char_class=?, level=?,
                            race_name=?, race_description=?,
                            class_description=?, appearance_description=?, backstory=?,
                            strength=?, dexterity=?, constitution=?, intelligence=?, wisdom=?, charisma=?,
                            armor_class=?, max_hp=?, current_hp=?, speed=?, proficiency_bonus=?,
                            hit_dice_total=?, hit_dice_remaining=?, hit_die_type=?
                        WHERE id=? AND user_id=?
                    """.trimIndent()

                    db.prepareStatement(sql).use { ps ->
                        var i = 1
                        ps.setString(i++, body.name)
                        ps.setString(i++, body.charClass)
                        ps.setInt(i++, body.level)
                        ps.setString(i++, body.raceName)
                        ps.setString(i++, body.raceDescription)
                        ps.setString(i++, body.classDescription)
                        ps.setString(i++, body.appearanceDescription)
                        ps.setString(i++, body.backstory)
                        ps.setInt(i++, body.strength)
                        ps.setInt(i++, body.dexterity)
                        ps.setInt(i++, body.constitution)
                        ps.setInt(i++, body.intelligence)
                        ps.setInt(i++, body.wisdom)
                        ps.setInt(i++, body.charisma)
                        ps.setInt(i++, body.armorClass)
                        ps.setInt(i++, body.maxHp)
                        ps.setInt(i++, body.currentHp)
                        ps.setInt(i++, body.speed)
                        ps.setInt(i++, body.proficiencyBonus)
                        ps.setInt(i++, body.hitDiceTotal)
                        ps.setInt(i++, body.hitDiceRemaining)
                        ps.setInt(i++, body.hitDieType)
                        ps.setInt(i++, characterId)
                        ps.setString(i, userId)
                        ps.executeUpdate()
                    }

                    val updated = selectCharacterById(characterId)!!.copy(userId = userId)
                    call.respond(updated)
                }


                delete("/characters/{characterId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()
                        ?: return@delete call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)


                    db.prepareStatement("SELECT user_id FROM characters WHERE id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) {
                                return@delete call.respondText("Character not found", status = io.ktor.http.HttpStatusCode.NotFound)
                            }
                            if (rs.getString("user_id") != userId) {
                                return@delete call.respondText("Not authorized to delete this character", status = io.ktor.http.HttpStatusCode.Forbidden)
                            }
                        }
                    }


                    db.prepareStatement("UPDATE characters SET campaign_id = NULL WHERE id = ? AND user_id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.setString(2, userId)
                        ps.executeUpdate()
                    }


                    db.prepareStatement("DELETE FROM characters WHERE id = ? AND user_id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.setString(2, userId)
                        val rows = ps.executeUpdate()
                        if (rows > 0) {
                            call.respondText("Character deleted successfully")
                        } else {
                            call.respondText("Failed to delete character", status = io.ktor.http.HttpStatusCode.InternalServerError)
                        }
                    }
                }


                get("/campaigns/{campaignId}/characters") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@get call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to view this campaign's characters", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@get
                    }

                    val characters = mutableListOf<Character>()
                    val sql = """
                        SELECT c.id, c.name, c.char_class, c.level, c.race_name, c.race_description,
                               c.class_description, c.appearance_description, c.backstory,
                               c.campaign_id, camp.name AS campaign_name,
                               c.strength, c.dexterity, c.constitution, c.intelligence, c.wisdom, c.charisma,
                               c.armor_class, c.max_hp, c.current_hp, c.speed, c.proficiency_bonus,
                               c.hit_dice_total, c.hit_dice_remaining, c.hit_die_type
                        FROM characters c
                        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
                        WHERE c.campaign_id = ?
                    """.trimIndent()

                    db.prepareStatement(sql).use { ps ->
                        ps.setString(1, campaignId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) characters.add(mapCharacter(rs))
                        }
                    }
                    call.respond(characters)
                }

                get("/campaigns/{campaignId}/characters/mine") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val campaignId = call.parameters["campaignId"] ?: return@get call.respondText(
                        "Campaign ID required", status = io.ktor.http.HttpStatusCode.BadRequest
                    )

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to view this campaign", status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@get
                    }

                    val characters = mutableListOf<Character>()
                    val sql = """
                        SELECT c.id, c.name, c.char_class, c.level,
                               c.race_name, c.race_description,
                               c.class_description, c.appearance_description, c.backstory,
                               c.campaign_id, camp.name AS campaign_name,
                               c.strength, c.dexterity, c.constitution, c.intelligence, c.wisdom, c.charisma,
                               c.armor_class, c.max_hp, c.current_hp, c.speed, c.proficiency_bonus,
                               c.hit_dice_total, c.hit_dice_remaining, c.hit_die_type
                        FROM characters c
                        LEFT JOIN campaigns camp ON c.campaign_id = camp.id
                        WHERE c.user_id = ? AND c.campaign_id = ?
                    """.trimIndent()

                    db.prepareStatement(sql).use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, campaignId)
                        ps.executeQuery().use { rs ->
                            while (rs.next()) characters.add(mapCharacter(rs, userId))
                        }
                    }

                    call.respond(characters)
                }

                put("/characters/{characterId}/assign/{campaignId}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterIdStr = call.parameters["characterId"]
                    val campaignId = call.parameters["campaignId"]
                    if (characterIdStr == null || campaignId == null) {
                        call.respondText("Character ID and Campaign ID required",
                            status = io.ktor.http.HttpStatusCode.BadRequest)
                        return@put
                    }
                    val characterId = characterIdStr.toInt()
                    val owns = db.prepareStatement(
                        "SELECT 1 FROM characters WHERE id = ? AND user_id = ?"
                    ).use { ps ->
                        ps.setInt(1, characterId)
                        ps.setString(2, userId)
                        ps.executeQuery().use { rs -> rs.next() }
                    }
                    if (!owns) {
                        call.respondText("Character not found or not owned by user",
                            status = io.ktor.http.HttpStatusCode.NotFound)
                        return@put
                    }

                    if (!isUserInCampaign(userId, campaignId)) {
                        call.respondText("Not authorized to assign character to this campaign",
                            status = io.ktor.http.HttpStatusCode.Forbidden)
                        return@put
                    }

                    val currentCampaign: String? = db.prepareStatement(
                        "SELECT campaign_id FROM characters WHERE id = ?"
                    ).use { ps ->
                        ps.setInt(1, characterId)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getString("campaign_id") else null }
                    }
                    if (currentCampaign == campaignId) {
                        call.respondText("Character is already assigned to this campaign")
                        return@put
                    }

                    if (userHasCharacterInCampaign(userId, campaignId, exceptCharacterId = characterId)) {
                        call.respondText(
                            "Bitte verlasse erst mit deinem Charakter die Kampagne.",
                            status = io.ktor.http.HttpStatusCode.Conflict
                        )
                        return@put
                    }

                    db.prepareStatement("UPDATE characters SET campaign_id = ? WHERE id = ?").use { ps ->
                        ps.setString(1, campaignId)
                        ps.setInt(2, characterId)
                        ps.executeUpdate()
                    }
                    call.respondText("Character assigned to campaign successfully")
                }

                put("/characters/{characterId}/unassign") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()
                    val characterId = call.parameters["characterId"]?.toIntOrNull()
                        ?: return@put call.respondText("Character ID required", status = io.ktor.http.HttpStatusCode.BadRequest)


                    val owns = db.prepareStatement("SELECT user_id FROM characters WHERE id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.executeQuery().use { rs -> if (rs.next()) rs.getString("user_id") == userId else false }
                    }
                    if (!owns) return@put call.respondText("Not authorized", status = io.ktor.http.HttpStatusCode.Forbidden)

                    db.prepareStatement("UPDATE characters SET campaign_id = NULL WHERE id = ?").use { ps ->
                        ps.setInt(1, characterId)
                        ps.executeUpdate()
                    }
                    call.respondText("Character unassigned from campaign")
                }
            }

        }
    }.start(wait = true)
}
