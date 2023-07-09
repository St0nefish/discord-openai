// support for ULong
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.APIUsage
import com.st0nefish.discord.openai.data.ChatExchange
import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.ImageExchange
import dev.kord.core.entity.User
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Slf4jSqlDebugLogger
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Instant
import kotlin.time.Duration.Companion.hours

/**
 * singleton database utility class to interact with the sqlite database holding historical query data
 *
 * @constructor
 *
 * @param config the bot config object
 */
class DatabaseUtils private constructor(config: Config = Config.instance()) {
    companion object {
        // logger
        private val log: Logger = LoggerFactory.getLogger(this::class.java)

        // singleton object
        @Volatile
        private var instance: DatabaseUtils? = null

        /**
         * instance function to get our singleton database object - instantiates it if not already done
         *
         * @return
         */
        @Synchronized
        fun instance(): DatabaseUtils {
            if (null == instance) {
                this.instance = DatabaseUtils()
            }
            return instance !!
        }

        /**
         * set instance to return - used for testing
         *
         * @param instance
         */
        @Synchronized
        fun setInstance(instance: DatabaseUtils) = instance.also { this.instance = it }
    }

    // config object
    private val config: Config

    /**
     * init block to connect to the database, set transaction level, and make sure our required tables exist
     */
    init { // store db path
        this.config = config // connect to sqlite database and configure isolation level
        Database.connect("jdbc:sqlite:${config.dbPath}", "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel =
            Connection.TRANSACTION_SERIALIZABLE // make sure the required tables exist
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            SchemaUtils.create(Conversations)
            SchemaUtils.create(Images)
        }
    }

    /**
     * conversations object used to create and manage the conversations table holding historical data about ChatGPT API
     * usage
     *
     * @constructor Create empty Conversations object to define columns and types
     */
    object Conversations : IntIdTable(name = "gpt_conversations", columnName = "id") {
        var author = ulong(name = "author").index()
        var conversationId = uuid(name = "conversation_id").index()
        var timestamp = timestamp(name = "timestamp").index()
        var success = bool(name = "success")
        var prompt = text(name = "prompt", eagerLoading = true)
        var response = text(name = "response", eagerLoading = true)
        var requestTokens = integer("request_tokens")
        var responseTokens = integer("response_tokens")
        var totalTokens = integer("total_tokens")
        var cost = double(name = "cost")
    }

    /**
     * save a chat exchange object to the conversations table
     *
     * @param chat ChatExchange object to save
     */
    fun saveChatExchange(chat: ChatExchange) {
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            Conversations.insert {
                it[author] = chat.author
                it[conversationId] = chat.conversationId
                it[timestamp] = chat.timestamp
                it[success] = chat.success
                it[prompt] = chat.prompt
                it[response] = chat.response
                it[requestTokens] = chat.requestTokens
                it[responseTokens] = chat.responseTokens
                it[totalTokens] = chat.totalTokens
                it[cost] = chat.cost
            }
        }
    }

    /**
     * get the most recent chat exchange object
     *
     * @param author optional user ID - if passed the most recent chat for that user is returned, otherwise the most
     * recent overall chat
     * @return the most recent chat exchange matching the input param
     */
    fun getLastChatExchange(author: ULong? = null): ChatExchange? {
        var exchange: ChatExchange? = null
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            val query = if (author != null) {
                Conversations.select { Conversations.author eq author }
            } else {
                Conversations.selectAll()
            }
            val latest = query.orderBy(Conversations.timestamp to SortOrder.DESC).limit(1).firstOrNull()
            if (latest?.hasValue(Conversations.id) == true) {
                exchange = ChatExchange(
                    latest[Conversations.author],
                    latest[Conversations.prompt],
                    latest[Conversations.response],
                    latest[Conversations.success],
                    latest[Conversations.requestTokens],
                    latest[Conversations.responseTokens],
                    latest[Conversations.totalTokens],
                    latest[Conversations.cost],
                    latest[Conversations.timestamp],
                    latest[Conversations.conversationId],
                    latest[Conversations.id].value,
                )
            }
        }
        return exchange
    }

    /**
     * get the total cost of chat interactions matching the input param
     *
     * @param user optional user ID to get cost total for. if not passed the overall total is returned
     * @return total cost of chat API usage matching the input param
     */
    private fun getChatCost(user: ULong? = null): Double {
        var cost = 0.0
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            val query = Conversations.slice(Conversations.cost.sum()).selectAll()
            user?.let {
                query.andWhere { Conversations.author eq user }
            }
            query.andWhere { Conversations.timestamp greater getOffsetTime() }
            cost = query.first()[Conversations.cost.sum()] ?: 0.0
        }
        return cost
    }

    /**
     * images object to create and manage the images table
     *
     * @constructor Create empty Images object defining columns and types
     */
    object Images : IntIdTable(name = "dalle_images", columnName = "id") {
        var author = ulong(name = "author").index()
        var imageId = uuid(name = "image_id")
        var timestamp = timestamp(name = "timestamp").index()
        var success = bool(name = "success")
        var prompt = text(name = "prompt", eagerLoading = true)
        var size = varchar(name = "size", 16)
        var url = text(name = "url", eagerLoading = true)
        var cost = double(name = "cost")
    }

    /**
     * save an image exchange interaction to the Images table
     *
     * @param image generated image exchange object to save
     */
    fun saveImageExchange(image: ImageExchange) {
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            Images.insert {
                it[author] = image.author
                it[prompt] = image.prompt
                it[size] = image.size
                it[url] = image.url
                it[success] = image.success
                it[cost] = image.cost
                it[timestamp] = image.timestamp
                it[imageId] = image.imageId
            }
        }
    }

    /**
     * get the most recent image generation exchange matching the input param
     *
     * @param author optional user ID to get an image for
     * @return most recent image interaction matching the input param
     */
    fun getLastImage(author: ULong? = null): ImageExchange? {
        var image: ImageExchange? = null
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            val query = if (author != null) {
                Images.select { Images.author eq author }
            } else {
                Images.selectAll()
            }
            val latest = query.orderBy(Images.timestamp to SortOrder.DESC).limit(1).firstOrNull()
            if (latest?.hasValue(Images.id) == true) {
                image = ImageExchange(
                    latest[Images.author],
                    latest[Images.size],
                    latest[Images.prompt],
                    latest[Images.url],
                    latest[Images.success],
                    latest[Images.cost],
                    latest[Images.timestamp],
                    latest[Images.imageId],
                    latest[Images.id].value)
            }
        }
        return image
    }

    /**
     * get the total cost of image generation API usage
     *
     * @param user optional user ID to get total cost for, otherwise overall total cost
     * @return total cost of image API usage matching the input param
     */
    private fun getImageCost(user: ULong? = null): Double {
        var cost = 0.0
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            val query = Images.slice(Images.cost.sum()).selectAll()
            user?.let {
                query.andWhere { Images.author eq user }
            }
            query.andWhere { Images.timestamp greater getOffsetTime() }
            cost = query.firstOrNull()?.get(Images.cost.sum()) ?: 0.0
        }
        return cost
    }

    /**
     * Get API usage stats matching the input params
     *
     * @param user optional user ID to get usage data for - if not passed returns overall usage for all users
     * @param timed if true returns API usage stats in the configured tracked time period, otherwise returns all-time
     * usage stats
     * @return APIUsage object matching the passed params
     */
    fun getAPIUsage(user: ULong? = null, timed: Boolean = false): APIUsage {
        val usage = APIUsage()
        transaction {
            addLogger(Slf4jSqlDebugLogger) // build queries
            val chatQuery = Conversations.slice(
                Conversations.requestTokens.sum(),
                Conversations.responseTokens.sum(),
                Conversations.totalTokens.sum(),
                Conversations.cost.sum()).selectAll()
            val imgQuery = Images.slice(Images.id.count(), Images.cost.sum()).selectAll()

            // if user is passed add filter
            user?.let {
                chatQuery.andWhere { Conversations.author eq user }
                imgQuery.andWhere { Images.author eq user }
            }

            // if time period is enabled add filter
            if (timed) {
                val cutoff = getOffsetTime()
                chatQuery.andWhere { Conversations.timestamp greater cutoff }
                imgQuery.andWhere { Images.timestamp greater cutoff }
            }

            // process chat results
            val chatResult = chatQuery.firstOrNull()
            usage.gptRequestTokens = chatResult?.get(Conversations.requestTokens.sum()) ?: 0
            usage.gptResponseTokens = chatResult?.get(Conversations.responseTokens.sum()) ?: 0
            usage.gptTotalTokens = chatResult?.get(Conversations.totalTokens.sum()) ?: 0
            usage.gptCost = chatResult?.get(Conversations.cost.sum()) ?: 0.0

            // process image results
            val imgResult = imgQuery.firstOrNull()
            usage.dalleImages = imgResult?.get(Images.id.count())?.toInt() ?: 0
            usage.dalleCost = imgResult?.get(Images.cost.sum()) ?: 0.0

            // set total cost
            usage.totalCost = usage.gptCost + usage.dalleCost
        }
        return usage
    }

    /**
     * check if the given user is allowed to make a request or if they've already passed the configured max daily cost
     * cap
     *
     * @param user User making the request
     * @return Boolean - can they make another request
     */
    fun canMakeRequest(user: User): Boolean {
        return if (user.id.value == config.owner.value) {
            // user is bot owner
            log.info("${user.tag} is bot owner - allowing request")
            true
        } else if (config.unlimitedUsers.contains(user.id.value)) {
            // user is in unlimited list
            log.info("${user.tag} is in unlimited list - allowing request")
            true
        } else {
            return if (getTimedCost(user.id.value) < config.maxCost) {
                // user has not exceeded daily limit
                log.info(
                    "${user.tag} has used ${
                        formatDollarString(getTimedCost(user.id.value))
                    } out of daily limit ${
                        formatDollarString(config.maxCost)
                    }")
                true
            } else { // user has exceeded daily limit
                log.info(
                    "user ${user.tag} has exceeded daily limit of ${
                        formatDollarString(config.maxCost)
                    }\n```\n${
                        getUsageString(user, config, getAPIUsage(user.id.value), getAPIUsage(user.id.value, true))
                    }\n```")
                false
            }
        }
    }

    /**
     * get the total cost of API usage over the tracked time period
     *
     * @param user ID of the user to get timed usage cost for
     * @return total API cost for the specified user over the tracked time period
     */
    private fun getTimedCost(user: ULong): Double {
        return getChatCost(user) + getImageCost(user)
    }

    /**
     * get the offset time as an Instant used as a filter condition for the configured tracking period
     *
     * @return an Instant object representing the earliest cutoff time in our tracked window
     */
    private fun getOffsetTime(): Instant {
        return Instant.now().minusSeconds(config.costInterval.hours.inWholeSeconds)
    }
}