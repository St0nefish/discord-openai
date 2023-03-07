package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.UsageTracker
import me.jakejmattson.discordkt.commands.commands

val usageTracker = UsageTracker.getInstance()

/**
 * Usage commands to allow all users to get their personal API usage data
 */
@Suppress("unused")
fun usageCommands() = commands("usage") {
    /**
     * get-usage-stats command to respond privately to a user with their current API usage stats
     */
    slash("get-usage-stats", "get your personal OpenAI API usage stats") {
        execute {
            respond(usageTracker.getUserUsageString(author))
        }
    }
}