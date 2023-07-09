package com.st0nefish.discord.openai.data

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.System.getenv
import java.util.stream.Collectors

/**
 * bot Config object to hold various configuration options
 */
@Serializable
data class Config(
    // path to config json
    private val configPath: String = getenv(ENV_CONFIG_PATH) ?: CONFIG_PATH,
    // discord bot token
    private val botToken: String = getenv(ENV_BOT_TOKEN),
    // id of the discord user who owns the bot (can configure it)
    val owner: Snowflake = Snowflake(getenv(ENV_BOT_OWNER).toULong()),
    // list of admin users
    val admins: MutableList<ULong> = csvToListULong(getenv(ENV_BOT_ADMINS) ?: ""),
    // id of the admin guild
    val adminGuild: Snowflake? = Snowflake(getenv(ENV_ADMIN_GUILD)),
    // path to usage database
    val dbPath: String = getenv(ENV_DB_PATH) ?: DB_PATH,
    // open ai api token
    val openAIToken: String = getenv(ENV_OPENAI_TOKEN) ?: "",
    // list of channels to allow messages in
    var allowChannels: MutableList<ULong> = csvToListULong(getenv(ENV_ALLOW_CHANNELS) ?: ""),
    // list of users to allow use via private message
    var allowUsers: MutableList<ULong> = csvToListULong(getenv(ENV_ALLOW_USERS) ?: ""),
    // list of users exempt from usage limits
    var unlimitedUsers: MutableList<ULong> = csvToListULong(getenv(ENV_UNLIMITED_USERS) ?: ""),
    // when a user is limited what is the max cost to allow per interval
    var maxCost: Double = getenv(USER_MAX_COST)?.toDouble() ?: MAX_COST,
    // when a user is limited what is the cost interval (in hours)
    var costInterval: Int = getenv(ENV_COST_TIME_INTERVAL)?.toInt() ?: COST_INTERVAL,
) {
    companion object {
        // json serializer
        private val serializer = Json { ignoreUnknownKeys = true }

        // default settings
        const val CONFIG_PATH = "./config/config.json"
        const val DB_PATH: String = "./config/data.db"
        const val MAX_COST: Double = 0.50
        const val COST_INTERVAL: Int = 24

        // singleton instance
        @Volatile
        private var instance: Config? = null

        /**
         * get the singleton config instance. instantiate if not already done
         *
         * @return config object
         */
        @Synchronized
        fun instance(): Config {
            if (null == instance) {
                instance = fromFile()
            }
            return instance !!
        }

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

        /**
         * parse a config object from the input json string
         *
         * @param json string to parse
         * @return config object
         */
        private fun fromJson(json: String): Config = serializer.decodeFromString<Config>(json)

        /**
         * create a config object from a json file at the target path
         *
         * @param path path to the config file
         * @return
         */
        private fun fromFile(path: String = CONFIG_PATH): Config { // parse to object if file existed
            val json = readConfigFile(path)
            return if (! json.isNullOrBlank()) {
                fromJson(json)
            } else {
                val config = Config()
                config.toFile()
                config
            }
        }

        /**
         * read config json from the file at the specified path
         *
         * @param path file path to read from
         * @return config object
         */
        private fun readConfigFile(path: String = CONFIG_PATH): String? {
            return if (File(path).exists()) { // read object from file
                File(path).readText()
            } else {
                null
            }
        }
    }

    /**
     * add a channel that the bot is allowed to talk in
     *
     * @param channelId ID of the channel to add
     */
    fun addAllowChannel(channelId: Snowflake) {
        allowChannels.add(channelId.value)
        this.toFile()
    }

    /**
     * add a user allowed to use the bot via direct message
     *
     * @param userId ID of the user to allow
     */
    fun addAllowUsers(userId: Snowflake) {
        allowUsers.add(userId.value)
        this.toFile()
    }

    /**
     * add a user who is exempt from the periodic cost limit
     *
     * @param userId ID of the user to add
     */
    fun addUnlimitedUser(userId: Snowflake) {
        unlimitedUsers.add(userId.value)
        this.toFile()
    }

    /**
     * return this config object as a formatted string for display and logging
     *
     * @return a formatted string representation of this Config object
     */
    override fun toString(): String { // build response str
        val formatStr = "%-20s%s"
        var response = ""
        response += formatStr.format("Owner ID:", owner.value)
        response += "%n".format()
        response += formatStr.format("Admin Guild:", adminGuild?.value)
        response += "%n".format()
        response += formatStr.format("Admins:", admins)
        response += "%n".format()
        response += formatStr.format("Allow Channels:", allowChannels)
        response += "%n".format()
        response += formatStr.format("Allow Users:", allowUsers)
        response += "%n".format()
        response += formatStr.format("Unlimited Users:", unlimitedUsers)
        response += "%n".format()
        response += formatStr.format("Discord Token:", botToken)
        response += "%n".format()
        response += formatStr.format("OpenAPI Token:", openAIToken)
        response += "%n".format()
        response += formatStr.format("Config Path:", configPath)
        response += "%n".format()
        response += formatStr.format("DB Path:", dbPath)
        response += "%n".format()
        response += formatStr.format("Max Cost:", maxCost)
        response += "%n".format()
        response += formatStr.format("Cost Interval:", costInterval)
        return response
    }

    /**
     * serialize this config object to a json file
     */
    fun toFile() = File(configPath).writeText(serializer.encodeToString(this))
}