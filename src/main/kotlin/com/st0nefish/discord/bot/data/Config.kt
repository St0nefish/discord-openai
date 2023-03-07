package com.st0nefish.discord.bot.data

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data
import java.util.stream.Collectors

@Serializable
class Config(
    private val botToken: String = System.getenv(EnvironmentVars.BOT_TOKEN),
    val owner: Snowflake = Snowflake("0".toULong()),
    val openAIToken: String = System.getenv(EnvironmentVars.OPENAI_TOKEN) ?: "",
    val prefix: String = System.getenv(EnvironmentVars.CMD_PREFIX) ?: "/",
    val allowGuilds: List<ULong> = getAllowList(System.getenv(EnvironmentVars.ALLOW_GUILDS) ?: ""),
    val allowChannels: List<ULong> = getAllowList(System.getenv(EnvironmentVars.ALLOW_CHANNELS) ?: ""),
) : Data() {
    companion object {
        fun getAllowList(guildListStr: String): List<ULong> {
            return if (guildListStr.isBlank()) {
                ArrayList()
            } else {
                guildListStr.split(",").stream().map { it.toULong() }.collect(Collectors.toList())
            }
        }
    }

    suspend fun asString(kord: Kord): String {
        // get bot owner user details
        val owner = kord.getUser(owner)
        // build response str
        val formatStr = "%-16s%s"
        var response = ""
        response += formatStr.format("Owner ID:", "${owner?.id}")
        response += "%n".format()
        response += formatStr.format("Owner Tag:", "${owner?.tag}")
        response += "%n".format()
        response += formatStr.format("Command Prefix:", prefix)
        response += "%n".format()
        response += formatStr.format("Discord Token:", botToken)
        response += "%n".format()
        response += formatStr.format("OpenAPI Token:", openAIToken)
        response += "%n".format()
        response += formatStr.format("Allow Guilds:", allowGuilds)
        response += "%n".format()
        response += formatStr.format("Allow Channels:", allowChannels)
        return response
    }
}