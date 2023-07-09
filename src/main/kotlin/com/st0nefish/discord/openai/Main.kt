package com.st0nefish.discord.openai

import com.st0nefish.discord.openai.commands.registerAdminCommands
import com.st0nefish.discord.openai.commands.registerChatCommands
import com.st0nefish.discord.openai.commands.registerImageCommands
import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.ENV_BOT_TOKEN
import com.st0nefish.discord.openai.data.ENV_CLEAN_START
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.main")

suspend fun main() { // load config
    // create kord object using token from environment variable
    val kord = Kord(System.getenv(ENV_BOT_TOKEN))

    // if enabled ? de-register all current commands
    if (System.getenv(ENV_CLEAN_START)?.toBoolean() == true) {
        deregisterAllCommands(kord)
    }

    // register commands
    registerAdminCommands(kord)
    registerChatCommands(kord)
    registerImageCommands(kord)

    // print startup config
    logStartup(kord)

    // login and listen
    kord.login() {
        name = "Aithena"
        intents = Intents.nonPrivileged
        presence {
            status = PresenceStatus.Online
            streaming("with OpenAI", "https://openai.com/")
        }
    }
}

/**
 * Deregister all commands
 *
 * @param kord current Kord object
 */
private suspend fun deregisterAllCommands(kord: Kord) {
    val applicationId = kord.resources.applicationId // deregister global commands
    val globalCommands = kord.rest.interaction.getGlobalApplicationCommands(applicationId)
    log.info("de-registering ${globalCommands.size} global commands...")
    globalCommands.forEach { globalCommand ->
        log.info("de-registering global command [${globalCommand.name}]")
        kord.rest.interaction.deleteGlobalApplicationCommand(applicationId, globalCommand.id)
    } // deregister guild commands for all guilds
    val guildCommands = kord.guilds.toList()
        .flatMap { guild -> kord.rest.interaction.getGuildApplicationCommands(applicationId, guild.id) }.toList()
    log.info("de-registering ${guildCommands.size} guild commands...")
    guildCommands.forEach { command ->
        log.info("de-registering guild [${command.guildId.value?.value}] command [${command.name}]")
        kord.rest.interaction.deleteGuildApplicationCommand(applicationId, command.guildId.value !!, command.id)
    }
}

/**
 * output startup logs
 *
 * @param kord
 */
private suspend fun logStartup(kord: Kord) {
    // print startup config
    println("----------------------------------")
    println(getBanner())
    println("----------------------------------")
    println("Using config:")
    println(Config.instance().toString().replaceIndent("  "))
    println("----------------------------------")
    println("Registered in guilds:")
    kord.guilds.toList().forEach { println(it.name.replaceIndent("  ")) }
    println("----------------------------------")
}

/**
 * output ascii banner
 *
 * @return ascii banner
 */
private fun getBanner(): String = """
   ____  _                       _ 
  |  _ \(_)___  ___ ___  _ __ __| |
  | | | | / __|/ __/ _ \| '__/ _` |
  | |_| | \__ \ (_| (_) | | | (_| |
  |____/|_|___/\___\___/|_|  \__,_|
                                               
   ___                      _    ___ 
  / _ \ _ __   ___ _ __    / \  |_ _|
 | | | | '_ \ / _ \ '_ \  / _ \  | | 
 | |_| | |_) |  __/ | | |/ ___ \ | | 
  \___/| .__/ \___|_| |_/_/   \_\___|
       |_|                                                                     
"""