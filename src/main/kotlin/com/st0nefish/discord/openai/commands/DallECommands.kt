package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.behavior.channel.createEmbed
import io.ktor.utils.io.*
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.commands.commands

/**
 * DALL·E commands
 */
@Suppress("unused")
fun dallECommands() = commands("DallE") {
    val openAI = OpenAIUtils.instance()


    /**
     * ask-dalle command that takes an image size and a prompt and will generate an image with DALL·E using those
     * parameters
     */
    slash("ask-dalle", "generate an image with DALL·E") {
        execute(
            ChoiceArg(
                "image-size",
                "Generated image size: Small(256x256), Medium(512x512), Large(1024x1024)",
                "256x256",
                "512x512",
                "1024x1024"
            ), AnyArg("Prompt", "prompt to use for image generation")
        ) {
            // capture user prompt and give immediate echo response
            val (size, prompt) = args
            respond("asking DALL·E for $size image with prompt: $prompt")

            // generate image via DALL·E
            val imageResponse = openAI.createImage(author, size, prompt)
            if (imageResponse.success) {
                val requestedBy = author
                channel.createEmbed {
                    title = prompt
                    field {
                        name = "author"
                        value = requestedBy.mention
                    }
                    field {
                        name = "size"
                        value = size
                    }
                    image = imageResponse.url
                }
            } else {
                channel.createMessage(
                    " ${author.mention} - failed to generate image with DALL·E\n```\n${imageResponse.url}\n```"
                )
            }
        }
    }
}