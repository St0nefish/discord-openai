group = "com.st0nefish"
version = "0.1.5"
description = "a discord bot for communicating with OpenAI"

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // json serialization plugin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
    // logger implementation
    implementation("org.slf4j:slf4j-simple:${Versions.slf4j}")
    // kord for discord api wrapper
    implementation("dev.kord:kord-core:${Versions.kord}")
    // openai api wrapper
    implementation("com.aallam.openai:openai-client:${Versions.openai}")
    // jetbrains database api
    implementation("org.jetbrains.exposed:exposed-core:${Versions.exposed}")
    implementation("org.jetbrains.exposed:exposed-dao:${Versions.exposed}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${Versions.exposed}")
    implementation("org.jetbrains.exposed:exposed-java-time:${Versions.exposed}")
    // sqlite implementation of exposed
    implementation("org.xerial:sqlite-jdbc:${Versions.sqlite}")
}

application {
    mainClass.set("com.st0nefish.discord.openai.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.java
}

tasks.compileJava {
    sourceCompatibility = Versions.java
    targetCompatibility = Versions.java
}

//tasks.register<WriteProperties>("writeProperties") {
//    property("name", project.name)
//    property("description", project.description.toString())
//    property("version", project.version)
//    property("url", "https://github.com/St0nefish/discord-gpt")
//    setOutputFile("src/main/resources/bot.properties")
//}

val fatJar = task<Jar>("fatJar") {
    description = "create an executable jar with all dependencies"
    group = "build"
    dependsOn.addAll(
        listOf("compileJava", "compileKotlin", "processResources"))
    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass))
    }
    val contents = configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    } + sourceSets.main.get().output
    from(contents)
}

tasks.build {
    dependsOn(fatJar)
}