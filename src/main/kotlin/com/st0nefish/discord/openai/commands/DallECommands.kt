package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.behavior.channel.createEmbed
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.ChoiceArg
import me.jakejmattson.discordkt.commands.commands
import org.slf4j.LoggerFactory

/**
 * DALL·E commands
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun dallECommands(config: Config) = commands("DallE") {
    val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.commands.DallECommands")
    val openAI = OpenAIUtils(config)

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
            // log
            log.info("${author.tag} asked DALL·E for $size image with prompt: $prompt")
            // generate image via DALL·E
            val imgURL: String
            try {
                imgURL = openAI.createImage(author, size, prompt)
            } catch (e: Exception) {
                // handle exception
                log.error(
                    "exception getting image from DALL·E %nAuthor: %s:%nPrompt:%s%nException:%n%s".format(
                        author.tag, prompt, e.stackTraceToString()
                    )
                )
                channel.createMessage(
                    "%s - failed to get image from DALL·E%nPrompt: %s%nException:%n```%s```".format(
                        author.mention, prompt, e.stackTraceToString()
                    )
                )
                return@execute
            }
            // log
            log.info("%s generated %s image via DALL·E%nPrompt: %s%nURL: %s".format(author.tag, size, prompt, imgURL))

            // return response to channel using embed
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
        }
    }
}