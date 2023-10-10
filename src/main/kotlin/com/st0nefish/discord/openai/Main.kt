package com.st0nefish.discord.openai

import com.st0nefish.discord.openai.commands.registerAdminCommands
import com.st0nefish.discord.openai.commands.registerChatCommands
import com.st0nefish.discord.openai.commands.registerGeneralCommands
import com.st0nefish.discord.openai.commands.registerImageCommands
import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.CommandManager
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList

// startup banner output to the logs
private const val startupBanner = """
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

// mandatory authentication tokens
private val discordToken = System.getenv(ENV_DISCORD_TOKEN)
private val openAiToken = System.getenv(ENV_OPENAI_TOKEN)

/**
 * main method to load configuration and start the bot process
 */
suspend fun main() {
    // Discord token is required
    if (discordToken.isNullOrBlank()) {
        throw NullPointerException("$ENV_DISCORD_TOKEN is required for running this bot")
    }
    // OpenAI token is required
    if (openAiToken.isNullOrBlank()) {
        throw NullPointerException("$ENV_OPENAI_TOKEN is required for running this bot")
    }

    // load bot config
    val config: Config = Config.instance()

    // log startup
    println("starting discord-openai bot...")
    println("Bot Name:      ${config.botName}")
    println("Discord Token: $discordToken")
    println("OpenAI Token:  $openAiToken")

    // create kord object using the configured Discord token
    val kord = Kord(discordToken)

    // if enabled ? de-register all current commands
    if (System.getenv(ENV_CLEAN_START).toBoolean()) {
        CommandManager.deregisterAllCommands(kord)
    }

    // register commands
    registerGeneralCommands(kord)
    registerChatCommands(kord)
    registerImageCommands(kord)
    registerAdminCommands(kord)

    // print startup config
    println(startupBanner)
    println("----------------------------------")
    println("Discord token: [${discordToken}]")
    println("OpenAI token:  [${openAiToken}]")
    println("----------------------------------")
    println("Using config:")
    println(Config.instance().toString().replaceIndent("  "))
    println("----------------------------------")
    println("Registered in guilds:")
    kord.guilds.toList().forEach { println(it.name.replaceIndent("  ")) }
    println("----------------------------------")

    // login and listen
    kord.login() {
        name = config.botName
        intents = Intents.nonPrivileged
        presence {
            status = PresenceStatus.Online
            streaming("with OpenAI", "https://github.com/St0nefish/discord-openai/")
        }
    }
}