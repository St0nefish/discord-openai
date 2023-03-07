package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.UsageTracker
import me.jakejmattson.discordkt.commands.commands

val usageTracker = UsageTracker.getInstance()

@Suppress("unused")
fun usageCommands() = commands("usage") {
    slash("get-usage-stats", "get your personal OpenAI API usage stats") {
        execute {
            respond(usageTracker.getUserUsageStr(author))
        }
    }
}