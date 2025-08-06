plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.0"
    application
}

application {
    mainClass.set("Server")
}

dependencies {
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)

    implementation(libs.sqlite.jdbc)
    implementation(libs.jbcrypt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
}
