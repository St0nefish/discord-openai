import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.st0nefish"
version = "1.0-SNAPSHOT"
description = "a discord bot for communicating with chat gpt"

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    application
}

repositories {
    mavenCentral()
}

dependencies {
    api("dev.kord:kord-core:${Versions.kord}")
    api("org.slf4j:slf4j-simple:${Versions.slf4j}")
    implementation("me.jakejmattson:DiscordKt:${Versions.discordkt}")
    implementation("com.aallam.openai:openai-client:${Versions.openai}")
    implementation("com.google.guava:guava:${Versions.guava}")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.register<WriteProperties>("writeProperties") {
    property("name", project.name)
    property("description", project.description.toString())
    property("version", project.version)
    property("url", "https://github.com/St0nefish/discord-gpt")
    setOutputFile("src/main/resources/bot.properties")
}

application {
    mainClass.set("MainKt")
}