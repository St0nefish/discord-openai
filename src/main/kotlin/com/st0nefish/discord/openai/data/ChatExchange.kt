package com.st0nefish.discord.openai.data

import com.st0nefish.discord.openai.data.APIUsage.Companion.formatDollarString
import java.time.Instant
import java.util.*

data class ChatExchange(
    val author: ULong,
    val model: String?,
    val prompt: String,
    var response: String = "",
    var success: Boolean = true,
    var requestTokens: Int = 0,
    var responseTokens: Int = 0,
    var totalTokens: Int = 0,
    var cost: Double = 0.0,
    var timestamp: Instant = Instant.now(),
    var conversationId: UUID = UUID.randomUUID(),
    private var rowId: Int = 0,
) {
    override fun toString(): String {
        return """
            row_id:             $rowId
            author:             $author
            conversation_id:    $conversationId
            timestamp:          ${Date.from(timestamp)}
            success:            $success
            request_tokens:     $requestTokens
            response_tokens:    $responseTokens
            total_tokens:       $totalTokens
            cost:               ${formatDollarString(cost)}
            prompt:             $prompt
            response:           ${response.trim()}
        """.trimIndent()
    }
}