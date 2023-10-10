package com.st0nefish.discord.openai.data

import java.time.Instant
import java.util.*

data class APIUsage(
    var gptRequestTokens: Int = 0,
    var gptResponseTokens: Int = 0,
    var gptTotalTokens: Int = 0,
    var gptCost: Double = 0.0,
    var dalleImages: Int = 0,
    var dalleCost: Double = 0.0,
    var totalCost: Double = 0.0,
    var user: ULong = ULong.MIN_VALUE,
    private var timestamp: Instant = Instant.now()) {
    /** static companion object */
    companion object {
        /**
         * get a formatted cost string
         *
         * @param cost value to convert to a string
         * @return
         */
        fun formatDollarString(cost: Double): String {
            return "$%,.5f".format(Locale.US, cost)
        }
    }

    /**
     * generate a string representation of this usage data object
     *
     * @return usage string
     */
    override fun toString(): String {
        return """
            timestamp:              ${Date.from(timestamp)}
            gpt_request_tokens:     $gptRequestTokens
            gpt_response_tokens:    $gptResponseTokens
            gpt_total_tokens:       $gptTotalTokens
            gpt_cost:               ${formatDollarString(gptCost)}
            dalle_images:           $dalleImages
            dalle_cost:             ${formatDollarString(dalleCost)}
            total_cost:             ${formatDollarString(totalCost)}
        """.trimIndent().trim()
    }
}