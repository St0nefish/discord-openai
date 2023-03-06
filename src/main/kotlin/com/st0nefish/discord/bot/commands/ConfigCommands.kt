package com.st0nefish.discord.bot.commands

import com.st0nefish.discord.bot.data.Config
import dev.kord.core.Kord
import me.jakejmattson.discordkt.commands.commands

fun dataCommands(config: Config) = commands("Config") {
    slash("get-config", "get current configuration") {
        execute {
            if (author.id.value == config.owner.value) {
                respond(getConfigMsg(config, discord.kord))
            } else {
                val owner = discord.kord.getUser(config.owner)
                respond("only my overlord, the almighty ${owner?.tag} may access this endpoint")
            }
        }
    }
}

suspend fun getConfigMsg(config: Config, kord: Kord): String {
    return """
       ```
       ${config.asString(kord)}
       ```
    """.trimIndent()
}