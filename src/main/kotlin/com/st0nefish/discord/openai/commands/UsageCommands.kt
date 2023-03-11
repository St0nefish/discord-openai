package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.DatabaseUtils
import com.st0nefish.discord.openai.utils.StringFormatUtils
import me.jakejmattson.discordkt.commands.commands

private val database = DatabaseUtils.instance()

/**
 * Usage commands to allow all users to get their personal API usage data
 */
@Suppress("unused")
fun usageCommands(config: Config) = commands("usage") {
    /**
     * get-usage-stats command to respond privately to a user with their current API usage stats
     */
    slash("get-usage-stats", "get your personal OpenAI API usage stats") {
        execute {
            respond(
                StringFormatUtils.getUsageString(
                    author, config, database.getAPIUsage(author.id.value), database.getAPIUsage(author.id.value, true)
                )
            )
        }
    }
}