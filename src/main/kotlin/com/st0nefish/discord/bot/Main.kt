package com.st0nefish.discord.bot

import com.st0nefish.discord.bot.data.Config
import com.st0nefish.discord.bot.data.EnvironmentVars
import dev.kord.common.annotation.KordPreview
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.locale.Language
import java.awt.Color


@KordPreview
fun main() {
    // discord bot token
    val token: String = System.getenv(EnvironmentVars.BOT_TOKEN)
    if (token.isBlank()) {
        throw BotConfigurationException("bot token is required to start the discord-gpt bot")
    }

    bot(token) {
        // load configuration
        val config = data("config/config.json") { Config() }

        // set prefix from config
        prefix { config.prefix }

        // configure bot options
        configure {
            mentionAsPrefix = true
            logStartup = true
            documentCommands = true
            recommendCommands = true
            searchCommands = true
            deleteInvocation = true
            dualRegistry = true
            commandReaction = null
            theme = Color(0x00BFFF)
            intents = Intents.nonPrivileged
        }

        presence {
            playing("Discord GPT")
        }

        onStart {
            println(config.asString(kord))
            println("-----------------------------")
            println("Registered in guilds:")
            kord.guilds.toList().forEach { println(it.name) }
            println("-----------------------------")
        }

        localeOf(Language.EN) {
            helpName = "help"
            helpCategory = "utility"
            commandRecommendation = "Recommendation: {0}"
        }
    }
}