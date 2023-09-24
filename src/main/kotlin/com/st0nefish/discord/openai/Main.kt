package com.st0nefish.discord.openai

import com.st0nefish.discord.openai.commands.deregisterAllCommands
import com.st0nefish.discord.openai.commands.registerAdminCommands
import com.st0nefish.discord.openai.commands.registerChatCommands
import com.st0nefish.discord.openai.commands.registerImageCommands
import com.st0nefish.discord.openai.data.Config
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.main")

suspend fun main() {
    // load bot config
    val config: Config = Config.instance()

    // log startup
    log.info("starting discord-openai bot using token [${config.botToken}]...")

    // create kord object using token from environment variable
    val kord = Kord(config.botToken)

    // if enabled ? de-register all current commands
    if (config.cleanStart) {
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