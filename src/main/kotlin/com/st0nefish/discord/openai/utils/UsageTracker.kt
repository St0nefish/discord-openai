package com.st0nefish.discord.openai.utils

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.st0nefish.discord.openai.data.Config
import dev.kord.core.entity.User
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class UsageTracker private constructor() {
    companion object {
        // costs
        const val TEXT_TOKEN_COST = 0.002 / 1000
        const val IMG_1024_COST = 0.02 / 1
        const val IMG_512_COST = 0.018 / 1
        const val IMG_256_COST = 0.016 / 1

        @Volatile
        private var instance: UsageTracker? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: UsageTracker().also { instance = it }
        }
    }

    private val log = LoggerFactory.getLogger(this.javaClass.name)

    // store configuration object
    private lateinit var config: Config

    // daily trackers use map of guava timed caches
    private val userDailyCostMap = mutableMapOf<ULong, Cache<String, Double>>()
    private val userDailyTokensMap = mutableMapOf<ULong, Cache<String, Int>>()
    private val userDailyImagesMap = mutableMapOf<ULong, Cache<String, Int>>()

    // total trackers just use a map
    private val userTotalCostMap = mutableMapOf<ULong, Double>()
    private val userTotalTokensMap = mutableMapOf<ULong, Int>()
    private val userTotalImagesMap = mutableMapOf<ULong, Int>()

    fun setConfig(config: Config) {
        this.config = config
    }

    fun canMakeRequest(user: User): Boolean {
        return (user.id.value == config.owner.value || getUserDailyCost(user) < config.maxDailyUserCost)
    }

    fun trackChatRequest(user: User, tokens: Int) {
        val userID = user.id.value
        // calculate cost of this request
        val thisCost = tokens * TEXT_TOKEN_COST
        // daily trackers
        userDailyCostMap.getOrPut(userID) { createTimedCache() }.put(UUID.randomUUID().toString(), thisCost)
        userDailyTokensMap.getOrPut(userID) { createTimedCache() }.put(UUID.randomUUID().toString(), tokens)
        // total trackers
        userTotalCostMap[userID] = getUserTotalCost(user) + thisCost
        userTotalTokensMap[userID] = this.getUserTotalTokens(user) + tokens
        // log
        logUsageStats(user)
    }

    fun trackImageRequest(user: User, size: String) {
        val userID = user.id.value
        // calculate cost of this image
        val imgCost = when (size) {
            "256x256" -> IMG_256_COST
            "512x512" -> IMG_512_COST
            "1024x1024" -> IMG_1024_COST
            else -> IMG_1024_COST
        }
        // daily trackers
        userDailyCostMap.getOrPut(userID) { createTimedCache() }.put(UUID.randomUUID().toString(), imgCost)
        userDailyImagesMap.getOrPut(userID) { createTimedCache() }.put(UUID.randomUUID().toString(), 1)
        // total trackers
        userTotalCostMap[userID] = getUserTotalCost(user) + imgCost
        userTotalImagesMap[userID] = getUserTotalImages(user) + 1
        // log
        logUsageStats(user)
    }

    fun getUserDailyCost(user: User): Double {
        return userDailyCostMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    fun getUserDailyTokens(user: User): Int {
        return userDailyTokensMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    fun getUserDailyImages(user: User): Int {
        return userDailyImagesMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    fun getUserUsageStr(user: User): String {
        return "API Usage for ${
            user.mention
        }:\n\tDaily:\n\t\tTokens: ${
            getUserDailyTokens(user)
        }\n\t\tImages:${
            getUserDailyImages(user)
        }\n\t\tCost: \$${
            getUserDailyCost(user)
        }\n\tTotal:\n\t\tTokens: ${
            getUserTotalTokens(user)
        }\n\t\tImages: ${
            getUserTotalImages(user)
        }\n\t\tCost: \$${
            getUserTotalCost(user)
        }"
    }

    fun getTotalUsageStr(): String {
        return "Total Usage API :\n\tDaily:\n\t\tTokens: ${
            getDailyTotalTokens()
        }\n\t\tImages:${
            getDailyTotalImages()
        }\n\t\tCost: \$${
            getDailyTotalCost()
        }\n\tTotal:\n\t\tTokens: ${
            getTotalTokens()
        }\n\t\tImages: ${
            getTotalImages()
        }\n\t\tCost: \$${
            getTotalCost()
        }"
    }

    private fun getUserTotalCost(user: User): Double {
        return userTotalCostMap.getOrDefault(user.id.value, 0.0)
    }

    private fun getUserTotalTokens(user: User): Int {
        return userTotalTokensMap.getOrDefault(user.id.value, 0)
    }

    private fun getUserTotalImages(user: User): Int {
        return userTotalImagesMap.getOrDefault(user.id.value, 0)
    }

    private fun <T> createTimedCache(): Cache<String, T> {
        return CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build()
    }

    private fun getDailyTotalCost(): Double {
        return userDailyCostMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    private fun getDailyTotalTokens(): Int {
        return userDailyTokensMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    private fun getDailyTotalImages(): Int {
        return userDailyImagesMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    private fun getTotalCost(): Double {
        return userTotalCostMap.values.sum()
    }

    private fun getTotalTokens(): Int {
        return userTotalTokensMap.values.sum()
    }

    private fun getTotalImages(): Int {
        return userTotalImagesMap.values.sum()
    }

    private fun logUsageStats(user: User) {
        // log
        log.info(
            "${user.tag} usage data:\n\tDaily: tokens=${getUserDailyTokens(user)}, images=${
                getUserDailyImages(user)
            }, cost=\$${getCostStr(getUserDailyCost(user))}\n\tTotal: tokens=${getUserTotalTokens(user)}, images=${
                getUserTotalImages(user)
            }, cost=\$${getCostStr(getUserTotalCost(user))}"
        )
    }

    private fun getCostStr(cost: Double): String {
        return "%,.4f".format(Locale.US, cost)
    }
}