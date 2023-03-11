package com.st0nefish.discord.openai

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.EnvironmentVars
import com.st0nefish.discord.openai.utils.DatabaseUtils
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList
import me.jakejmattson.discordkt.dsl.bot
import me.jakejmattson.discordkt.locale.Language
import java.awt.Color

/**
 * create a Discord bot for communicating with OpenAI APIs
 */
@KordPreview
fun main() {
    // create bot using token retrieved from environment variables
    bot(System.getenv(EnvironmentVars.BOT_TOKEN)) {
        // load configuration
        val config = data("config/config.json") { Config() }

        // initialize dbUtils
        DatabaseUtils.init(config)

        // initialize openAIUtils
        OpenAIUtils.init(config)

        // set prefix from config
        prefix { config.cmdPrefix }

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

        // set presence to online and streaming OpenAI - not ideal but no hook to custom presence
        presence {
            status = PresenceStatus.Online
            streaming("with OpenAI", "https://openai.com/")
        }

        // log some config details on startup
        onStart {
            println(config.asString(kord))
            println("-----------------------------")
            println("Registered in guilds:")
            kord.guilds.toList().forEach { println(it.name) }
            println("-----------------------------")
        }

        // set locale
        localeOf(Language.EN) {
            helpName = "help"
            helpCategory = "utility"
            commandRecommendation = "Recommendation: {0}"
        }
    }
}