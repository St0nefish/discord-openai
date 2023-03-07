group = "com.st0nefish"
version = "0.1.2"
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
    implementation("dev.kord:kord-core:${Versions.kord}")
    implementation("org.slf4j:slf4j-simple:${Versions.slf4j}")
    implementation("me.jakejmattson:DiscordKt:${Versions.discordkt}")
    implementation("com.aallam.openai:openai-client:${Versions.openai}")
    implementation("com.google.guava:guava:${Versions.guava}")
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

tasks.register<WriteProperties>("writeProperties") {
    property("name", project.name)
    property("description", project.description.toString())
    property("version", project.version)
    property("url", "https://github.com/St0nefish/discord-gpt")
    setOutputFile("src/main/resources/bot.properties")
}

val fatJar = task<Jar>("fatJar") {
    description = "create an executable jar with all dependencies"
    group = "build"
    dependsOn.addAll(
        listOf("compileJava", "compileKotlin", "processResources")
    )
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

//tasks {
//    val fatJar = register<Jar>("fatJar") {
//        dependsOn.addAll(
//            listOf(
//                "compileJava", "compileKotlin", "processResources"
//            )
//        ) // We need this for Gradle optimization to work
//        archiveClassifier.set("standalone") // Naming the jar
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//        manifest {
//            attributes(mapOf("Main-Class" to application.mainClass))
//        } // Provided we set it up in the application plugin configuration
//        val sourcesMain = sourceSets.main.get()
//        val contents =
//            configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output
//        from(contents)
//    }
//    build {
//        dependsOn(fatJar) // Trigger fat jar creation during build
//    }
//}