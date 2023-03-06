package com.st0nefish.discord.bot.data

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data

@Serializable
class Config(
    val owner: Snowflake = Snowflake(System.getenv(EnvironmentVars.OWNER).toULong()),
    val prefix: String = System.getenv(EnvironmentVars.CMD_PREFIX),
    val botToken: String = System.getenv(EnvironmentVars.BOT_TOKEN),
    val openAIToken: String = System.getenv(EnvironmentVars.OPENAI_TOKEN)
) : Data() {
    suspend fun asString(kord: Kord): String {
        // get bot owner user details
        val owner = kord.getUser(owner)
        // build response str
        val formatStr = "%-16s%s"
        var response = ""
        response += formatStr.format("Owner ID:", "${owner?.id} )")
        response += "%n".format()
        response += formatStr.format("Owner Name:", "${owner?.username}(#${owner?.discriminator})")
        response += "%n".format()
        response += formatStr.format("Command Prefix:", prefix)
        response += "%n".format()
        response += formatStr.format("Bot Token:", botToken)
        response += "%n".format()
        response += formatStr.format("OpenAPI Token:", openAIToken)
        return response
    }
}