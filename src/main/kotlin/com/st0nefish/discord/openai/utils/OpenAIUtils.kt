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
import com.st0nefish.discord.openai.data.Config
import dev.kord.core.entity.User
import io.ktor.util.*
import java.util.*

/**
 * utility class for communicating with the OpenAI API
 *
 * @constructor create a new OpenAIUtils class
 *
 * @param config bot config object
 */
@OptIn(BetaOpenAI::class)
class OpenAIUtils(config: Config) {
    // create our openAI client
    private val openAI = OpenAI(config.openAIToken)

    // usage tracker
    private val usageTracker = UsageTracker.getInstance()

    /**
     * request a basic text completion from Chat GPT for a single input prompt
     *
     * @param user User who is requesting the completion
     * @param prompt String chat completion prompt
     * @return response from chat GPT
     */
    suspend fun basicCompletion(user: User, prompt: String): String {
        // check if user can make a request
        if (usageTracker.canMakeRequest(user)) {
            // create request
            val chatRequest = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"), messages = listOf(ChatMessage(role = ChatRole.User, content = prompt))
            )

            // execute request
            val completion: ChatCompletion = openAI.chatCompletion(chatRequest)

            // track api costs
            usageTracker.trackChatRequest(user, completion.usage?.totalTokens ?: 0)

            // return response
            return completion.choices.first().message?.content ?: "gpt did not respond"
        } else {
            return "${
                user.mention
            } exceeded API usage limit - current total: ${
                usageTracker.getUserDailyTokens(user)
            } tokens, ${
                usageTracker.getUserDailyImages(user)
            } images, \$$%,.4f".format(
                Locale.US, usageTracker.getUserDailyCost(user)
            )
        }
    }

    /**
     * generate an image via DALL·E
     *
     * @param user User who requested the image
     * @param size size of the image to generate
     * @param prompt String prompt to create an image for
     * @return URL of the generated image
     */
    suspend fun createImage(user: User, size: String, prompt: String): String {
        // check if user is allowed to make a request
        if (usageTracker.canMakeRequest(user)) {
            // parse size arg to ImageSize object
            val imageSize = ImageSize(size)

            // send request to DALL·E
            val images = openAI.imageURL(
                creation = ImageCreation(
                    n = 1, size = imageSize, prompt = prompt,
                )
            )

            // track cost of this request
            usageTracker.trackImageRequest(user, size)

            // return image url
            return images.first().url
        } else {
            return "${user.mention} exceeded API usage limit - current total: ${
                usageTracker.getUserDailyTokens(user)
            }, $%.4f".format(
                usageTracker.getUserDailyCost(user)
            )
        }
    }
}