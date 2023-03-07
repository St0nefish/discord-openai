package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.Constants
import com.st0nefish.discord.openai.utils.UsageTracker
import me.jakejmattson.discordkt.commands.commands

@Suppress("unused")
fun configCommands(config: Config) = commands(Constants.ADMIN_CATEGORY) {
    val usageTracker = UsageTracker.getInstance()

    slash("admin-get-config", "admin command to get bot configuration") {
        execute {
            respond("```${config.asString(discord.kord)}```")
        }
    }

    slash("admin-total-usage", "get total OpenAI API usage stats") {
        execute {
            respondPublic(usageTracker.getTotalUsageStr())
        }
    }

    user(
        displayText = "get API usage",
        slashName = "admin-user-usage",
        description = "get OpenAPI usage stats for this user"
    ) {
        respondPublic(usageTracker.getUserUsageStr(arg))
    }
}