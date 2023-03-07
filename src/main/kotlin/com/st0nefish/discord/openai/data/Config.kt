package com.st0nefish.discord.openai.data

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data
import java.util.stream.Collectors

/**
 * bot Config object to hold various options
 *
 * @property botToken the bot token required to connect this bot to Discord
 * @property owner the Snowflake ID of the Discord user who is the owner of this bot
 * @property openAIToken the token required to connect to the OpenAI API
 * @property prefix the command prefix to use for commands created by this bot - defaults to /
 * @property allowGuilds list of guild IDs this bot is allowed to receive commands from - defaults to empty
 * @property allowChannels list of channel IDs this bot is allowed to receive commands from - defaults to empty
 * @property maxDailyUserCost maximum daily API spend (in dollars) for each user per day
 * @constructor Create a Config object
 */
@Serializable
class Config(
    private val botToken: String = System.getenv(EnvironmentVars.BOT_TOKEN),
    val owner: Snowflake = Snowflake((System.getenv(EnvironmentVars.BOT_OWNER) ?: "0").toULong()),
    val openAIToken: String = System.getenv(EnvironmentVars.OPENAI_TOKEN) ?: "",
    val prefix: String = System.getenv(EnvironmentVars.CMD_PREFIX) ?: "/",
    val allowGuilds: List<ULong> = getAllowList(System.getenv(EnvironmentVars.ALLOW_GUILDS) ?: ""),
    val allowChannels: List<ULong> = getAllowList(System.getenv(EnvironmentVars.ALLOW_CHANNELS) ?: ""),
    val maxDailyUserCost: Double = (System.getenv(EnvironmentVars.MAX_DAILY_USER_COST) ?: "0.50").toDouble(),
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

    /**
     * return this config object as a formatted string for display and logging
     *
     * @param kord the Kord API object used to get user data from the Snowflake ID
     * @return a formatted string representation of this Config object
     */
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
        response += formatStr.format("User Daily Cost:", maxDailyUserCost)
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