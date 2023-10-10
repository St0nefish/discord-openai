package com.st0nefish.discord.openai.data

import com.st0nefish.discord.openai.ENV_ADMIN_GUILDS
import com.st0nefish.discord.openai.ENV_ADMIN_USERS
import com.st0nefish.discord.openai.ENV_ALLOW_CHANNELS
import com.st0nefish.discord.openai.ENV_ALLOW_USERS
import com.st0nefish.discord.openai.ENV_BOT_NAME
import com.st0nefish.discord.openai.ENV_COST_INTERVAL
import com.st0nefish.discord.openai.ENV_COST_MAX
import com.st0nefish.discord.openai.ENV_PATH_CONFIG
import com.st0nefish.discord.openai.ENV_UNLIMITED_USERS
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.System.getenv
import java.util.stream.Collectors

/**
 * bot Config object to hold various configuration options
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Config(
    // name of the bot
    @SerialName("bot_name")
    @EncodeDefault
    val botName: String = getenv(ENV_BOT_NAME) ?: DEFAULT_BOT_NAME,
    // list of admin users
    @SerialName("admin_users")
    @EncodeDefault
    val adminUsers: MutableSet<ULong> = csvToSetULong(getenv(ENV_ADMIN_USERS) ?: ""),
    // list of admin guilds
    @SerialName("admin_guilds")
    @EncodeDefault
    val adminGuilds: MutableSet<ULong> = csvToSetULong(getenv(ENV_ADMIN_GUILDS) ?: ""),
    // list of channels to allow messages in
    @SerialName("allow_channels")
    @EncodeDefault
    var allowChannels: MutableSet<ULong> = csvToSetULong(getenv(ENV_ALLOW_CHANNELS) ?: ""),
    // list of users to allow use via private message
    @SerialName("allow_users")
    @EncodeDefault
    var allowUsers: MutableSet<ULong> = csvToSetULong(getenv(ENV_ALLOW_USERS) ?: ""),
    // list of users exempt from usage limits
    @SerialName("unlimited_users")
    @EncodeDefault
    var unlimitedUsers: MutableSet<ULong> = csvToSetULong(getenv(ENV_UNLIMITED_USERS) ?: ""),
    // when a user is limited what is the max cost to allow per interval
    @SerialName("usage_cost_value")
    @EncodeDefault
    var usageCostValue: Double = getenv(ENV_COST_MAX)?.toDouble() ?: DEFAULT_COST_MAX,
    // when a user is limited what is the cost interval (in hours)
    @SerialName("usage_cost_interval")
    @EncodeDefault
    var usageCostInterval: Int = getenv(ENV_COST_INTERVAL)?.toInt() ?: DEFAULT_COST_INTERVAL,
) {
    // static companion object
    companion object {
        // logger
        private val LOG = LoggerFactory.getLogger(Config::class.java)

        // default settings
        const val DEFAULT_BOT_NAME = "Aithena"
        const val DEFAULT_COST_MAX: Double = 1.0 // $1.00
        const val DEFAULT_COST_INTERVAL: Int = 168 // per week

        // path to config file
        private val configPath: String = getenv(ENV_PATH_CONFIG) ?: "./config/config.json"

        // singleton instance
        @Volatile
        private var instance: Config? = null

        // json serializer
        private val serializer = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            prettyPrintIndent = "  "
        }

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
            return instance!!
        }

        /**
         * set the instance to use system-wide
         *
         * @param instance bot configuration object
         */
        @Synchronized
        fun setInstance(instance: Config) = instance.also { this.instance = it }

        /**
         * convert a CSV string to a MutableList of ULong
         *
         * @param csvStr
         * @return
         */
        fun csvToSetULong(csvStr: String): MutableSet<ULong> {
            return if (csvStr.isBlank()) {
                HashSet()
            } else {
                csvStr.split(",").stream().map { it.toULong() }.collect(Collectors.toSet())
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
        private fun fromFile(path: String = configPath): Config {
            // parse to object if file existed
            val json = readConfigFile(path)
            return if (!json.isNullOrBlank()) {
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
        private fun readConfigFile(path: String = configPath): String? {
            return if (File(path).exists()) {
                // read object from file
                File(path).readText()
            } else {
                null
            }
        }
    }

    /**
     * add an admin user
     *
     * @param user {@link ULong} ID of the user to add to admins list
     * @return latest admin user list
     */
    fun addAdminUser(user: ULong): Set<ULong> {
        adminUsers.add(user)
        this.toFile()
        return adminUsers
    }

    /**
     * add an admin guild
     *
     * @param guild {@link ULong} ID of the guild to add to the admins list
     * @return latest admin guild list
     */
    fun addAdminGuild(guild: ULong): Set<ULong> {
        adminGuilds.add(guild)
        this.toFile()
        return adminGuilds
    }

    /**
     * add a channel that the bot is allowed to talk in
     *
     * @param channelId ID of the channel to add
     * @return latest allowed channel list
     */
    fun addAllowChannel(channelId: ULong): Set<ULong> {
        allowChannels.add(channelId)
        this.toFile()
        return allowChannels
    }

    /**
     * add a user allowed to use the bot via direct message
     *
     * @param userId ID of the user to allow
     * @return latest allowed user list
     */
    fun addAllowUsers(userId: ULong): Set<ULong> {
        allowUsers.add(userId)
        this.toFile()
        return allowUsers
    }

    /**
     * add a user who is exempt from the periodic cost limit
     *
     * @param userId ID of the user to add
     * @return latest unlimited user list
     */
    fun addUnlimitedUser(userId: ULong): Set<ULong> {
        unlimitedUsers.add(userId)
        this.toFile()
        return unlimitedUsers
    }

    /**
     * Set a new usage limit
     *
     * @param cost {@link Double} max allowable cost per interval
     * @param interval {@link Int} number of hours per interval
     * @return updated usage string
     */
    fun setUsageLimit(cost: Double, interval: Int): String {
        this.usageCostValue = cost
        this.usageCostInterval = interval
        this.toFile()
        return getUsageString()
    }

    /**
     * return this config object as a formatted string for display and logging
     *
     * @return a formatted string representation of this Config object
     */
    override fun toString(): String {
        // build response str
        val formatStr = "%-20s%s"
        var response = ""
        response += formatStr.format("Admin Users:", adminUsers)
        response += "%n".format()
        response += formatStr.format("Admin Guilds:", adminGuilds)
        response += "%n".format()
        response += formatStr.format("Allow Channels:", allowChannels)
        response += "%n".format()
        response += formatStr.format("Allow Users:", allowUsers)
        response += "%n".format()
        response += formatStr.format("Unlimited Users:", unlimitedUsers)
        response += "%n".format()
        response += formatStr.format("Usage Limit:", getUsageString())
        return response
    }

    /**
     * return this config object as a formatted JSON string
     *
     * @return formatted JSON string for this object
     */
    fun toJson(): String {
        return serializer.encodeToString(this)
    }

    /**
     * serialize this config object to a json file
     */
    fun toFile() {
        val configFile = File(configPath)
        if (!configFile.exists()) {
            LOG.info("config file $configPath not found... creating")
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
        }
        configFile.writeText(serializer.encodeToString(this))
    }

    /**
     * get the usage config as a friendly string
     *
     * @return the usage string
     */
    private fun getUsageString(): String = "${APIUsage.formatDollarString(usageCostValue)}/$usageCostInterval hrs"
}