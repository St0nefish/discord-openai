import org.apache.tools.ant.filters.ReplaceTokens

group = "com.st0nefish"
version = "0.1.6"
description = "a discord bot for communicating with OpenAI"

plugins {
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.serialization") version Versions.KOTLIN
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // json serialization plugin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.SERIALIZATION}")
    // logger implementation
    implementation("org.slf4j:slf4j-simple:${Versions.SLF4J}")
    // kord for discord api wrapper
    implementation("dev.kord:kord-core:${Versions.KORD}")
    // openai api wrapper
    implementation("com.aallam.openai:openai-client:${Versions.OPENAI}")
    // jetbrains database api
    implementation("org.jetbrains.exposed:exposed-core:${Versions.EXPOSED}")
    implementation("org.jetbrains.exposed:exposed-dao:${Versions.EXPOSED}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${Versions.EXPOSED}")
    implementation("org.jetbrains.exposed:exposed-java-time:${Versions.EXPOSED}")
    // sqlite implementation of exposed
    implementation("org.xerial:sqlite-jdbc:${Versions.SQLITE}")
}

application {
    mainClass.set("com.st0nefish.discord.openai.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = Versions.JAVA
}

tasks.compileJava {
    sourceCompatibility = Versions.JAVA
    targetCompatibility = Versions.JAVA
}

tasks.register<Copy>("dockerCopyDist") {
    description = "copy the distribution to the docker build directory"
    group = "docker"
    dependsOn("distTar")

    // build distribution tar and copy to build/docker/discord-openai.tar
    from(tasks.distTar.get())
    into(layout.buildDirectory.dir("docker"))
}

tasks.register<Copy>("dockerCopyLogger") {
    description = "copy the logger config to the docker build directory"
    group = "docker"

    // copy logger config to build/docker
    from(layout.projectDirectory.file("docker/simplelogger.properties"))
    into(layout.buildDirectory.dir("docker"))
}

tasks.register<Copy>("generateDockerfile") {
    description = "generate dockerfile in docker build directory"
    group = "docker"

    // gradle properties
    val dockerBaseImg: String by project
    val dockerImgVersion: String = project.version.toString()

    // generate dockerfile into build/docker
    from(layout.projectDirectory.file("docker/Dockerfile"))
    into(layout.buildDirectory.dir("docker"))
    filter(
        ReplaceTokens::class, "tokens" to mapOf(
            "docker_base_image" to dockerBaseImg, "project_version" to dockerImgVersion))
}

tasks.register<Exec>("buildDockerImage") {
    description = "build the docker image"
    group = "docker"
    dependsOn("dockerCopyDist", "dockerCopyLogger", "generateDockerfile")

    // build docker image
    workingDir(layout.buildDirectory.dir("docker"))
    commandLine(
        "docker",
        "build",
        "-t",
        "st0nefish/${project.name}:${project.version}",
        "-t",
        "st0nefish/${project.name}:latest",
        ".")
}

tasks.register<Exec>("pushDockerImage") {
    description = "push the latest docker image"
    group = "docker"
    dependsOn("buildDockerImage")

    // push docker image
    workingDir(layout.buildDirectory.dir("docker"))
    commandLine("docker", "push", "st0nefish/${project.name}:${project.version}")
}

// create fat jar with all dependencies
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