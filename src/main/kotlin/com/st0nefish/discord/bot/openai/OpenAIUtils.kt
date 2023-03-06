package com.st0nefish.discord.bot.openai

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.st0nefish.discord.bot.data.Config
import io.ktor.util.*
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(BetaOpenAI::class)
class OpenAIUtils(private val config: Config) {
    // cost values
    companion object {
        const val TEXT_TOKEN_COST = 0.002 / 1000
        const val IMG_1024_COST = 0.02 / 1
        const val IMG_512_COST = 0.018 / 1
        const val IMG_256_COST = 0.016 / 1
        const val MAX_DAILY_USER_COST = 0.50
    }

    // create our openAI client
    private val openAI = OpenAI(config.openAIToken)

    // create a map to store cost per user
    private val costMap = mutableMapOf<ULong, Cache<String, Double>>()

    // basic text completion using gpt-3.5-turbo model
    suspend fun basicCompletion(user: ULong, prompt: String): String {
        // get current API cost for this user
        val currentCost: Double = costMap.getOrDefault(user, createTimedCostCache()).asMap().values.sum()
        println("user $user total current API cost: $%.4f".format(currentCost))

        // check if user can make a request
        if (canMakeRequest(user, currentCost)) {
            try {// create chat request
                val chatRequest = ChatCompletionRequest(
                    model = ModelId("gpt-3.5-turbo"), messages = listOf(
                        ChatMessage(
                            role = ChatRole.User, content = prompt
                        )
                    )
                )

                // execute request
                val completion: ChatCompletion = openAI.chatCompletion(chatRequest)

                // track api costs
                val thisCost = (completion.usage?.totalTokens ?: 0) * TEXT_TOKEN_COST
                println("user $user request cost: $thisCost")
                costMap.getOrPut(user) { createTimedCostCache() }.put(completion.id, thisCost)

                // return response
                return completion.choices.first().message?.content ?: "gpt did not respond"
            } catch (e: Exception) {
                println("Exception executing request:%n%s".format(e))
                return "Exception executing request \"%s\": %s".format(prompt, e.message)
            }
        } else {
            return "exceeded max allowable 24h API costs - current total: $%.4f".format(currentCost)
        }
    }

    // image completion using DALL·E
    suspend fun createImage(user: ULong, size: String, prompt: String): String {
        // get current API cost for this user
        val currentCost: Double = costMap.getOrDefault(user, createTimedCostCache()).asMap().values.sum()
        println("user $user total current API cost: $%.4f".format(currentCost))

        // check if user is allowed to make a request
        if (canMakeRequest(user, currentCost)) {
            // get image size
            var imageSize: ImageSize = ImageSize.is256x256
            when (size.toLowerCasePreservingASCIIRules()) {
                "small" -> imageSize = ImageSize.is256x256
                "medium" -> imageSize = ImageSize.is512x512
                "large" -> imageSize = ImageSize.is1024x1024
            }
            try {// generate image URL
                val images = openAI.imageURL(
                    creation = ImageCreation(
                        prompt = prompt, n = 1, size = imageSize
                    )
                )

                // track cost of this request
                costMap.getOrPut(user) { createTimedCostCache() }.put(UUID.randomUUID().toString(), IMG_1024_COST)

                // return image url
                return images.first().url
            } catch (e: Exception) {
                println("Exception generating %s image:%n%s".format(size, e))
                return "Exception generating %s image:%n%s".format(size, e)
            }
        } else {
            return "exceeded max allowable 24h API costs - current total: $%.4f".format(currentCost)
        }
    }

    private fun createTimedCostCache(): Cache<String, Double> {
        return CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build()
    }

    private fun canMakeRequest(user: ULong, cost: Double): Boolean {
        return (user == config.owner.value || cost < MAX_DAILY_USER_COST)
    }
}