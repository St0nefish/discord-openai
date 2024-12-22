package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.ENV_PATH_DB
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.System.getenv
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
class DatabaseUtils private constructor(private val config: Config = Config.instance()) {
    companion object {
        // logger
        private val log: Logger = LoggerFactory.getLogger(DatabaseUtils::class.java)

        // path to db file
        private val dbPath: String = getenv(ENV_PATH_DB) ?: "./config/data.db"

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
            return instance!!
        }

        /**
         * set instance to return - used for testing
         *
         * @param instance
         */
        @Synchronized
        fun setInstance(instance: DatabaseUtils) = instance.also { this.instance = it }
    }

    /**
     * init block to connect to the database, set transaction level, and make sure our required tables exist
     */
    init {
        // connect to our db object
        Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
        // set transaction mode
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        // make sure the required tables exist
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            // ensure schema is up to date
            SchemaUtils.createMissingTablesAndColumns(Conversations, Images)
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
        var model = text(name = "model", eagerLoading = true).nullable()
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
                it[model] = chat.model
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
                Conversations.selectAll().where { Conversations.author eq author }
            } else {
                Conversations.selectAll()
            }
            val latest = query.orderBy(Conversations.timestamp to SortOrder.DESC).limit(1).firstOrNull()
            if (latest?.hasValue(Conversations.id) == true) {
                exchange = ChatExchange(
                    author = latest[Conversations.author],
                    model = latest[Conversations.model],
                    prompt = latest[Conversations.prompt],
                    response = latest[Conversations.response],
                    success = latest[Conversations.success],
                    requestTokens = latest[Conversations.requestTokens],
                    responseTokens = latest[Conversations.responseTokens],
                    totalTokens = latest[Conversations.totalTokens],
                    cost = latest[Conversations.cost],
                    timestamp = latest[Conversations.timestamp],
                    conversationId = latest[Conversations.conversationId],
                    rowId = latest[Conversations.id].value,
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
            val query = Conversations.select(Conversations.cost.sum())
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
        var imageId = uuid(name = "image_id")
        var author = ulong(name = "author").index()
        var timestamp = timestamp(name = "timestamp").index()
        var success = bool(name = "success")
        var prompt = text(name = "prompt", eagerLoading = true)
        var model = text(name = "model", eagerLoading = true).nullable()
        var quality = text(name = "quality", eagerLoading = true).nullable()
        var style = text(name = "style", eagerLoading = true).nullable()
        var size = varchar(name = "size", 16)
        var url = text(name = "url", eagerLoading = true)
        var cost = double(name = "cost")
        var exception = text(name = "exception", eagerLoading = true).nullable()
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
                it[imageId] = image.imageId
                it[author] = image.author
                it[timestamp] = image.timestamp
                it[success] = image.success
                it[prompt] = image.prompt
                it[model] = image.model
                it[quality] = image.quality
                it[style] = image.style
                it[size] = image.size
                it[url] = image.url
                it[cost] = image.cost
                it[exception] = image.exception
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
                Images.selectAll().where { Images.author eq author }
            } else {
                Images.selectAll()
            }
            val latest = query.orderBy(Images.timestamp to SortOrder.DESC).limit(1).firstOrNull()
            if (latest?.hasValue(Images.id) == true) {
                image = ImageExchange(
                    author = latest[Images.author],
                    prompt = latest[Images.prompt],
                    model = latest[Images.model],
                    quality = latest[Images.quality],
                    style = latest[Images.style],
                    size = latest[Images.size],
                    url = latest[Images.url],
                    success = latest[Images.success],
                    cost = latest[Images.cost],
                    timestamp = latest[Images.timestamp],
                    imageId = latest[Images.imageId],
                    exception = latest[Images.exception],
                    rowId = latest[Images.id].value
                )
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
            val query = Images.select(Images.cost.sum())
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
            val chatQuery = Conversations.select(
                Conversations.requestTokens.sum(), Conversations.responseTokens.sum(), Conversations.totalTokens.sum(),
                Conversations.cost.sum()
            )
            val imgQuery = Images.select(Images.id.count(), Images.cost.sum())

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
        return when {
            // admin users always allowed
            AccessManager.isAdminUser(user.id.value) -> {
                log.info("${user.tag} is bot owner - allowing request")
                true
            }

            // if user is in unlimited use list allow
            AccessManager.isUnlimitedUser(user.id.value) -> {
                log.info("${user.tag} is in unlimited list - allowing request")
                true
            }

            // if user has not violated usage limit allow
            getTimedCost(user.id.value) < config.usageCostValue -> {
                log.info("user ${user.tag} has not yet exceeded timed usage cap - allowing request")
                true
            }

            // all other cases reject
            else -> {
                log.info("user ${user.tag} has exceeded daily usage limit - denying request")
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
        return Instant.now().minusSeconds(config.usageCostInterval.hours.inWholeSeconds)
    }
}