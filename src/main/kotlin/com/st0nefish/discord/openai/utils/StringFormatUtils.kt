package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.APIUsage
import com.st0nefish.discord.openai.data.Config
import dev.kord.core.entity.User
import java.util.*

/**
 * Get a formatted cost string
 *
 * @param cost value to convert to a string
 * @return
 */
fun formatDollarString(cost: Double): String {
    return "$%,.5f".format(Locale.US, cost)
}


/**
 * return a nicely formatted user string for both total and timed api usage stats
 *
 * @param user user to show stats for
 * @param config config object
 * @param totalUsage APIUsage object with lifetime data
 * @param timedUsage APIUsage object with timed data
 * @return
 */
fun getUsageString(user: User, config: Config, totalUsage: APIUsage, timedUsage: APIUsage): String {
    return "API usage for ${user.tag}:\n```\nTotal:\n${
        totalUsage.toString().replaceIndent("    ")
    }\nLast ${config.costInterval} hours:\n${
        timedUsage.toString().replaceIndent("    ")
    }\n```".trimIndent()
}