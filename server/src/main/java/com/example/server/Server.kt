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

val db = DriverManager.getConnection("jdbc:sqlite:data.db")
val secret = "very-secure-key"

fun main() {
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
    }

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
//        .withExpiresAt(Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
        .withExpiresAt((Date(System.currentTimeMillis() + 1000 * 30))) // 30 seconds (for testing)
        .sign(Algorithm.HMAC256(secret))
}
