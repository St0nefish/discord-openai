package com.st0nefish.discord.openai.data

import com.st0nefish.discord.openai.utils.StringFormatUtils
import java.time.Instant
import java.util.*

class ImageExchange(
    val author: ULong,
    val size: String,
    val prompt: String,
    var url: String = "",
    var success: Boolean = true,
    var cost: Double = 0.0,
    var timestamp: Instant = Instant.now(),
    var imageId: UUID = UUID.randomUUID(),
    private var rowId: Int = 0
) {
    override fun toString(): String {
        return """
            row_id:         $rowId
            author:         $author
            image_id:       $imageId
            timestamp:      ${Date.from(timestamp)}
            success:        $success
            cost:           ${StringFormatUtils.formatDollarString(cost)}
            size:           $size
            prompt:         $prompt
            url:            ${url.trim()}
        """.trimIndent()
    }
}