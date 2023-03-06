package com.st0nefish.discord.bot.commands

import com.st0nefish.discord.bot.data.Config
import com.st0nefish.discord.bot.data.Constants
import me.jakejmattson.discordkt.commands.commands

@Suppress("unused")
fun configCommands(config: Config) = commands(Constants.ADMIN_CATEGORY) {
    slash("admin-get-config", "admin command to get bot configuration") {
        execute {
            respond("```${config.asString(discord.kord)}```")
        }
    }
}