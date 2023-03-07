package com.st0nefish.discord.openai.utils

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.st0nefish.discord.openai.data.Config
import dev.kord.core.entity.User
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Usage tracker class to keep track of who is using the API, the number of text completion tokens they use, and the
 * number of images they generate. used to prevent any one user from going crazy and for general stats
 *
 * @constructor Create empty Usage tracker
 */
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

    /**
     * Set the bot config object
     *
     * @param config Config object for this bot
     */
    fun setConfig(config: Config) {
        this.config = config
    }

    /**
     * check if the given user is allowed to make a request or if they've already passed the configured max daily cost
     * cap
     *
     * @param user User making the request
     * @return Boolean - can they make another request
     */
    fun canMakeRequest(user: User): Boolean {
        return (user.id.value == config.owner.value || getUserDailyCost(user) < config.maxDailyUserCost)
    }

    /**
     * Track a chat request
     *
     * @param user User who made the request
     * @param tokens number of tokens consumed by that chat completion
     */
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

    /**
     * Track an image request
     *
     * @param user User who requested the image to be generated
     * @param size size of the image
     */
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

    /**
     * Get user daily cost
     *
     * @param user User to get daily cost data for
     * @return total spend for this user over the past 24 hours
     */
    fun getUserDailyCost(user: User): Double {
        return userDailyCostMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    /**
     * Get user daily tokens
     *
     * @param user User to get token count for
     * @return total number of tokens used over the past 24 hours
     */
    fun getUserDailyTokens(user: User): Int {
        return userDailyTokensMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    /**
     * Get user daily images
     *
     * @param user User to get image count for
     * @return total number of images generated over the past 24 hours
     */
    fun getUserDailyImages(user: User): Int {
        return userDailyImagesMap.getOrPut(user.id.value) { createTimedCache() }.asMap().values.sum()
    }

    /**
     * Get user usage string
     *
     * @param user User to get usage string for
     * @return a string with all current usage data for the specified user
     */
    fun getUserUsageString(user: User): String {
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

    /**
     * Get total usage string
     *
     * @return a string with all total usage data since last reboot
     */
    fun getTotalUsageString(): String {
        return "Total Usage API :\n\tDaily:\n\t\tTokens: ${
            getDailyTotalTokens()
        }\n\t\tImages:${
            getDailyTotalImages()
        }\n\t\tCost: ${
            getDailyTotalCost()
        }\n\tTotal:\n\t\tTokens: ${
            getTotalTokens()
        }\n\t\tImages: ${
            getTotalImages()
        }\n\t\tCost: ${
            getTotalCost()
        }"
    }

    /**
     * Get user total cost
     *
     * @param user User to get cost data for
     * @return total cost of API usage for this user since last reboot
     */
    private fun getUserTotalCost(user: User): Double {
        return userTotalCostMap.getOrDefault(user.id.value, 0.0)
    }

    /**
     * Get user total tokens
     *
     * @param user User to get token count for
     * @return total number of tokens used by this user since last reboot
     */
    private fun getUserTotalTokens(user: User): Int {
        return userTotalTokensMap.getOrDefault(user.id.value, 0)
    }

    /**
     * Get user total images
     *
     * @param user User to get image count for
     * @return total number of images generated by this user since last reboot
     */
    private fun getUserTotalImages(user: User): Int {
        return userTotalImagesMap.getOrDefault(user.id.value, 0)
    }

    /**
     * Create a timed cache object for tracking 24 hour usage
     *
     * @param T object type of the payload of the returned cache
     * @return a timed cache object that will expire an object 1 day after each entry was written
     */
    private fun <T> createTimedCache(): Cache<String, T> {
        return CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build()
    }

    /**
     * Get daily total cost
     *
     * @return the total cost of API usage over the last 24 hours
     */
    private fun getDailyTotalCost(): Double {
        return userDailyCostMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    /**
     * Get daily total tokens
     *
     * @return the total number of text tokens used over the last 24 hours
     */
    private fun getDailyTotalTokens(): Int {
        return userDailyTokensMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    /**
     * Get daily total images
     *
     * @return the total number of images generated over the last 24 hours
     */
    private fun getDailyTotalImages(): Int {
        return userDailyImagesMap.values.stream().map { it.asMap() }.flatMap { it.values.stream() }
            .collect(Collectors.toList()).sum()
    }

    /**
     * Get total cost
     *
     * @return total cost of API usage since last reboot
     */
    private fun getTotalCost(): Double {
        return userTotalCostMap.values.sum()
    }

    /**
     * Get total tokens
     *
     * @return the total number of text tokens used since last reboot
     */
    private fun getTotalTokens(): Int {
        return userTotalTokensMap.values.sum()
    }

    /**
     * Get total images
     *
     * @return the total number of images generated over the last 24 hours
     */
    private fun getTotalImages(): Int {
        return userTotalImagesMap.values.sum()
    }

    /**
     * Log usage stats for a given user
     *
     * @param user User to log stats for
     */
    private fun logUsageStats(user: User) {
        // log
        log.info(
            "${user.tag} usage data:\n\tDaily: tokens=${
                getUserDailyTokens(user)
            }, images=${
                getUserDailyImages(user)
            }, cost=${
                getCostString(getUserDailyCost(user))
            }\n\tTotal: tokens=${
                getUserTotalTokens(user)
            }, images=${
                getUserTotalImages(user)
            }, cost=${
                getCostString(getUserTotalCost(user))
            }"
        )
    }

    /**
     * Get a formatted cost string
     *
     * @param cost value to convert to a string
     * @return
     */
    private fun getCostString(cost: Double): String {
        return "$%,.4f".format(Locale.US, cost)
    }
}