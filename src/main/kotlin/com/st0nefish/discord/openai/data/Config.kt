package com.st0nefish.discord.openai.data

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.serialization.Serializable
import me.jakejmattson.discordkt.dsl.Data
import java.lang.System.getenv
import java.util.stream.Collectors

/**
 * bot Config object to hold various options
 *
 * @property botToken the bot token required to connect this bot to Discord
 * @property owner the Snowflake ID of the Discord user who is the owner of this bot
 * @property openAIToken the token required to connect to the OpenAI API
 * @property cmdPrefix the command prefix to use for commands created by this bot - defaults to /
 * @property allowGuilds list of guild IDs this bot is allowed to receive commands from - defaults to empty
 * @property allowChannels list of channel IDs this bot is allowed to receive commands from - defaults to empty
 * @property maxCost maximum daily API spend (in dollars) for each user per day
 * @property costInterval number of hours to track usage data for to compare to maxCost and prevent API usage
 * @constructor Create a Config object
 */
@Serializable
class Config(
    private val botToken: String = getenv(EnvironmentVars.BOT_TOKEN),
    val owner: Snowflake = Snowflake(getenv(EnvironmentVars.BOT_OWNER).toULong()),
    val dbPath: String = getenv(EnvironmentVars.DB_PATH) ?: DB_PATH,
    val cmdPrefix: String = getenv(EnvironmentVars.CMD_PREFIX) ?: CMD_PREFIX,
    val openAIToken: String = getenv(EnvironmentVars.OPENAI_TOKEN) ?: "",
    var allowChannels: MutableList<ULong> = csvToListULong(getenv(EnvironmentVars.ALLOW_CHANNELS) ?: ""),
    var unlimitedUsers: MutableList<ULong> = csvToListULong(getenv(EnvironmentVars.UNLIMITED_USERS) ?: ""),
    var maxCost: Double = getenv(EnvironmentVars.USER_MAX_COST)?.toDouble() ?: MAX_COST,
    var costInterval: Int = getenv(EnvironmentVars.COST_TIME_INTERVAL)?.toInt() ?: COST_INTERVAL,
) : Data() {
    companion object {
        // default settings
        const val DB_PATH: String = "./config/data.db"
        const val CMD_PREFIX: String = "/"
        const val MAX_COST: Double = 0.50
        const val COST_INTERVAL: Int = 24

        /**
         * convert a CSV string to a MutableList of ULong
         *
         * @param guildListStr
         * @return
         */
        fun csvToListULong(guildListStr: String): MutableList<ULong> {
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
        response += formatStr.format("Command Prefix:", cmdPrefix)
        response += "%n".format()
        response += formatStr.format("User Daily Cost:", maxCost)
        response += "%n".format()
        response += formatStr.format("Discord Token:", botToken)
        response += "%n".format()
        response += formatStr.format("OpenAPI Token:", openAIToken)
        response += "%n".format()
        response += formatStr.format("Allow Channels:", allowChannels)
        response += "%n".format()
        response += formatStr.format("Unlimited Users:", unlimitedUsers)
        return response
    }
}