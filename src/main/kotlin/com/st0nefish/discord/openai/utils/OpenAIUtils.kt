package com.st0nefish.discord.openai.utils

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.st0nefish.discord.openai.data.ChatExchange
import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.ImageExchange
import dev.kord.core.entity.User
import org.slf4j.LoggerFactory

/**
 * utility class for communicating with the OpenAI API
 *
 * @constructor create a new OpenAIUtils class
 *
 * @param config bot config object
 */
@OptIn(BetaOpenAI::class)
class OpenAIUtils private constructor(
    private val config: Config = Config.instance(), private val database: DatabaseUtils = DatabaseUtils.instance()) {
    companion object {
        // constants
        private const val TEXT_TOKEN_COST = 0.002 / 1000
        private const val IMG_1024_COST = 0.02 / 1
        private const val IMG_512_COST = 0.018 / 1
        private const val IMG_256_COST = 0.016 / 1

        /**
         * instance object for singleton management
         */
        @Volatile
        private var instance: OpenAIUtils? = null

        /**
         * instance function to get our singleton database object
         *
         * @return
         */
        @Synchronized
        fun instance(): OpenAIUtils {
            if (null == instance) {
                this.instance = OpenAIUtils()
            }
            return instance !!
        }

        /**
         * set instance to use
         *
         * @param instance
         */
        @Synchronized
        fun setInstance(instance: OpenAIUtils) = instance.also { this.instance = it }
    }

    // logger
    private val log = LoggerFactory.getLogger(OpenAIUtils::class.java)

    // openAI API
    private val openAI: OpenAI = OpenAI(config.openAIToken)

    /**
     * request a basic text completion from Chat GPT for a single input prompt
     *
     * @param user User who is requesting the completion
     * @param prompt String chat completion prompt
     * @return response from chat GPT
     */
    suspend fun getChatResponse(user: User, prompt: String): ChatExchange { // log question
        log.info("${user.tag} asked: $prompt") // create exchange object for return

        // create chat exchange
        val exchange = ChatExchange(user.id.value, prompt) // check if user can make a request
        if (database.canMakeRequest(user)) { // execute request
            val completion: ChatCompletion
            try {
                completion = openAI.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId("gpt-3.5-turbo"), messages = listOf(
                            ChatMessage(
                                role = ChatRole.User, content = prompt)))) // store successful result
                exchange.success = true
                exchange.response = completion.choices.first().message?.content?.trim() ?: "GPT returned no response"
                exchange.requestTokens = completion.usage?.promptTokens ?: 0
                exchange.responseTokens = completion.usage?.completionTokens ?: 0
                exchange.totalTokens = completion.usage?.totalTokens ?: 0
                exchange.cost = getChatCost(exchange.totalTokens) // log response
                log.info(
                    "GPT response:%nAuthor: %s%nPrompt: %s%nResponse: %s".format(
                        user.tag, prompt, exchange.response.trim()))
            } catch (e: Exception) { // handle exception
                exchange.success = false
                exchange.response = e.stackTraceToString()
            }
        } else { // user exceeded usage cap
            exchange.success = false
            exchange.response = """
               exceeded API usage limit of \$${config.maxCost} per ${config.costInterval} hours:
               ```
               ${database.getAPIUsage(user.id.value, true)}
               ```
            """.trimIndent()
            log.error("Failed to get response from GPT for user ${user.tag}:\n${exchange.response}")
        } // save exchange
        database.saveChatExchange(exchange) // return response
        return exchange
    }

    /**
     * generate an image via DALL·E
     *
     * @param user User who requested the image
     * @param size size of the image to generate
     * @param prompt String prompt to create an image for
     * @return URL of the generated image
     */
    suspend fun createImage(user: User, size: String, prompt: String): ImageExchange { // log
        log.info("${user.tag} asked DALL·E for $size image with prompt: $prompt")

        // create image object for return
        val image = ImageExchange(user.id.value, size, prompt) // check if user is allowed to make a request
        if (database.canMakeRequest(user)) {
            try { // send request to DALL·E
                val images = openAI.imageURL(
                    creation = ImageCreation(n = 1, size = ImageSize(size), prompt = prompt))
                image.success = true
                image.url = images.first().url
                image.cost = getImageCost(size)
                log.info(
                    "%s generated %s image via DALL·E%nPrompt: %s%nURL: %s".format(
                        user.tag, size, prompt, image.url))
            } catch (e: Exception) { // catch failure
                image.success = false
                image.url = e.stackTraceToString()
                log.error(
                    "exception getting image from DALL·E%nAuthor: %s:%nPrompt:%s%nException:%n%s".format(
                        user.tag, prompt, e.stackTraceToString()))
            }
        } else { // handle case where user has violated cap
            image.success = false
            image.url = """
               exceeded API usage limit of \$${config.maxCost} per ${config.costInterval} hours:
               ```
               ${database.getAPIUsage(user.id.value, true)}
               ```
            """.trimIndent()
        } // save image results and return
        database.saveImageExchange(image)
        return image
    }

    /**
     * get the cost of a chat request
     *
     * @param tokens number of tokens used by the request
     * @return cost of the request
     */
    private fun getChatCost(tokens: Int): Double {
        return tokens * TEXT_TOKEN_COST
    }

    /**
     * get the cost of an image request
     *
     * @param size size of the generated image
     * @return cost of the request
     */
    private fun getImageCost(size: String): Double {
        return when (size) {
            "256x256" -> IMG_256_COST
            "512x512" -> IMG_512_COST
            "1024x1024" -> IMG_1024_COST
            else -> IMG_1024_COST
        }
    }
}