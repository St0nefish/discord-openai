package com.st0nefish.discord.openai.utils

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.image.ImageURL
import com.aallam.openai.api.image.Quality
import com.aallam.openai.api.image.Style
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.st0nefish.discord.openai.ENV_OPENAI_TOKEN
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
class OpenAIUtils private constructor(
    private val config: Config = Config.instance(),
    private val database: DatabaseUtils = DatabaseUtils.instance(),
    openaiToken: String = System.getenv(ENV_OPENAI_TOKEN)!!
) {
    companion object {
        // gpt input costs per token
        private val gpt_input_cost: Map<String, Double> = mapOf(
            "gpt-4o-mini" to (0.15 / 1000000),
            "gpt-4o" to (2.50 / 1000000),
            "gpt-3.5-turbo-1106" to (1.00 / 1000000),
            "o1-mini" to (3.00 / 1000000),
            "o1" to (15.00 / 1000000),
        )

        // gpt output costs per token
        private val gpt_output_cost: Map<String, Double> = mapOf(
            "gpt-4o-mini" to (0.60 / 1000000),
            "gpt-4o" to (10.00 / 1000000),
            "gpt-3.5-turbo-1106" to (2.00 / 1000000),
            "o1-mini" to (12.00 / 1000000),
            "o1" to (60.00 / 1000000),
        )

        // dall-e cost
        private val dall_e_cost: Map<String, Map<String, Double>> = mapOf(
            "dall-e-3-standard" to mapOf(
                "1024x1024" to 0.040,
                "1024×1792" to 0.080,
                "1792x1024" to 0.080,
            ),
            "dall-e-3-hd" to mapOf(
                "1024x1024" to 0.080,
                "1024×1792" to 0.120,
                "1792x1024" to 0.120,
            ),
            "dall-e-2" to mapOf(
                "256x256" to 0.016,
                "512x512" to 0.018,
                "1024x1024" to 0.020,
            )
        )

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
            return instance!!
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
    private val log = LoggerFactory.getLogger(this::class.java)

    // openAI API
    private val openAI: OpenAI = OpenAI(openaiToken)

    /**
     * request a basic text completion from ChatGPT for a single input prompt
     *
     * @param user User who is requesting the completion
     * @param prompt String chat completion prompt
     * @return response from ChatGPT
     */
    suspend fun getChatResponse(user: User, prompt: String, model: String): ChatExchange { // log question
        log.info("${user.tag} asked: $prompt") // create exchange object for return
        // create chat exchange
        val exchange = ChatExchange(
            author = user.id.value,
            model = model,
            prompt = prompt
        ) // check if user can make a request
        if (database.canMakeRequest(user)) { // execute request
            val completion: ChatCompletion
            try {
                completion = openAI.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId(model), messages = listOf(
                            ChatMessage(role = ChatRole.User, content = prompt)
                        )
                    )
                ) // store successful result
                exchange.success = true
                exchange.response = completion.choices.first().message.content?.trim() ?: "GPT returned no response"
                exchange.requestTokens = completion.usage?.promptTokens ?: 0
                exchange.responseTokens = completion.usage?.completionTokens ?: 0
                exchange.totalTokens = completion.usage?.totalTokens ?: 0
                exchange.cost = getChatCost(model, exchange.requestTokens, exchange.responseTokens) // log response
                log.info(
                    "GPT response:%nAuthor: %sModel: %s%nCost: %s%nPrompt: %s%nResponse: %s".format(
                        user.tag, model, exchange.cost, prompt, exchange.response.trim()
                    )
                )
            } catch (e: Exception) { // handle exception
                exchange.success = false
                exchange.response = e.stackTraceToString()
            }
        } else { // user exceeded usage cap
            exchange.success = false
            exchange.response = """
               exceeded API usage limit of \$${config.usageCostValue} per ${config.usageCostInterval} hours:
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
    suspend fun createImage(
        user: User,
        prompt: String,
        model: String = "dall-e-3",
        size: String = "1024x1024",
        quality: String = "standard",
        style: String = "natural",
        count: Int = 1,
    ): ImageExchange { // log
        log.info("${user.tag} asked DALL·E for $size image with prompt: $prompt")
        // create image object for return
        val image = ImageExchange(
            author = user.id.value,
            model = model,
            quality = quality,
            style = style,
            size = size,
            prompt = prompt
        )
        // check if user is allowed to make a request
        if (database.canMakeRequest(user)) {
            try { // send request to DALL·E
                var images: List<ImageURL> = mutableListOf()
                if (model == "dall-e-3") {
                    images = openAI.imageURL(
                        creation = ImageCreation(
                            model = ModelId(model),
                            quality = Quality(quality),
                            style = Style(style),
                            size = ImageSize(size),
                            prompt = prompt,
                            n = count,
                        )
                    )
                } else if (model == "dall-e-2") {
                    images = openAI.imageURL(
                        creation = ImageCreation(
                            model = ModelId(model),
                            size = ImageSize(size),
                            n = count,
                            prompt = prompt
                        )
                    )
                }
                image.success = true
                image.url = images.first().url
                image.cost = getImageCost(model, quality, size, count)
                log.info("generated image via DALL·E\n${image}")
            } catch (e: Exception) { // catch failure
                image.success = false
                image.exception = e.stackTraceToString()
                log.error("exception generating image via DALL·E\n${image}\n${e.stackTraceToString()}")
            }
        } else { // handle case where user has violated cap
            image.success = false
            image.url = """
               exceeded API usage limit of \$${config.usageCostValue} per ${config.usageCostInterval} hours:
               ```
               ${database.getAPIUsage(user.id.value, true)}
               ```
            """.trimIndent()
        } // save image results and return
        database.saveImageExchange(image)
        return image
    }

    /**
     * get the cost of a chat request for a given model, input token count, and output token count
     *
     * @param model name of the model used
     * @param inTokens number of input tokens
     * @param outTokens number of output tokens
     * @return cost of the request
     */
    private fun getChatCost(model: String, inTokens: Int, outTokens: Int): Double {
        return (inTokens * (gpt_input_cost[model] ?: 0.01)) + (outTokens * (gpt_output_cost[model] ?: 0.02))
    }

    /**
     * get the cost of an image request
     *
     * @param size size of the generated image
     * @return cost of the request
     */
    private fun getImageCost(model: String, quality: String, size: String, count: Int): Double {
        var modelKey: String = model
        if (model == "dall-e-3") {
            modelKey = "dall-e-3-$quality"
        }
        return (dall_e_cost[modelKey]!![size] ?: 0.10) * count
    }
}