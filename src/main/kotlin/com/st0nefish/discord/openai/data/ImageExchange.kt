package com.st0nefish.discord.openai.data

import com.st0nefish.discord.openai.data.APIUsage.Companion.formatDollarString
import java.time.Instant
import java.util.*

data class ImageExchange(
    val author: ULong,
    val prompt: String,
    val model: String?,
    val quality: String?,
    val style: String?,
    val size: String,
    var url: String = "",
    var success: Boolean = true,
    var cost: Double = 0.0,
    var timestamp: Instant = Instant.now(),
    var imageId: UUID = UUID.randomUUID(),
    var exception: String? = null,
    private var rowId: Int = 0
) {
    override fun toString(): String {
        return """
            row_id:         $rowId
            author:         $author
            model:          $model
            image_id:       $imageId
            timestamp:      ${Date.from(timestamp)}
            success:        $success
            cost:           ${formatDollarString(cost)}
            size:           $size
            prompt:         $prompt
            url:            ${url.trim()}
            exception:      $exception
        """.trimIndent()
    }
}