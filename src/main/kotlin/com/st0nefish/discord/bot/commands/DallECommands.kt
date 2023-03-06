package com.st0nefish.discord.bot.commands

import com.st0nefish.discord.bot.data.Config
import com.st0nefish.discord.bot.openai.OpenAIUtils
import dev.kord.core.behavior.channel.createEmbed
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.commands.commands
import org.slf4j.LoggerFactory

@Suppress("unused")
fun dallECommands(config: Config) = commands("DallE") {
    val log = LoggerFactory.getLogger(this.javaClass.name)
    val openAI = OpenAIUtils(config)

    slash("ask-dalle", "generate an image with DALL·E") {
        execute(
            ChoiceArg(
                "image-size",
                "Generated image size: Small(256x256), Medium(512x512), Large(1024x1024)",
                "small",
                "medium",
                "large"
            ), AnyArg("Prompt", "prompt to use for image generation")
        ) {
            // capture user prompt and give immediate echo response
            val (size, prompt) = args
            respond("asking DALL·E for $size image with prompt: $prompt")
            // log
            println("${author.tag} asked DALL·E for $size image with prompt: $prompt")
            log.info("${author.tag} asked DALL·E for $size image with prompt: $prompt")
            try {
                // generate image URL
                val imgURL = openAI.createImage(author.id.value, size, prompt)
                // log
                println("${author.tag} generated $size image via DALL·E: $imgURL")
                log.info("${author.tag} generated $size image via DALL·E: $imgURL")
                // return response to channel with prompt echo
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
                    image = imgURL
                }
            } catch (e: Exception) {
                channel.createMessage("failed to generate image with prompt: \"$prompt\"\nexception:```${e.message}```")
            }
        }
    }
}