package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.OpenAIUtils
import com.st0nefish.discord.openai.utils.allowStandardAccess
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed
import org.slf4j.LoggerFactory

// constants
private const val IMG_SIZE = "image-size"
private const val IMG_PROMPT = "prompt"

// logger
private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.registerImageCommands")

/**
 * register image commands
 *
 * @param kord
 * @param openAI
 */
suspend fun registerImageCommands(kord: Kord, openAI: OpenAIUtils = OpenAIUtils.instance()) {
    // command config
    val cmd = "ask-dalle"


    // register image command
    kord.createGlobalChatInputCommand(cmd, "send a prompt to chat GPT") {
        string(IMG_SIZE, "Generated image size") {
            required = true
            choice("256x256", "256x256")
            choice("512x512", "512x512")
            choice("1024x1024", "1024x1024")
        }
        string(IMG_PROMPT, "the prompt to send to DALL-E") {
            required = true
        }
        log.info("registered command: $name")
    }

    // handle image command
    kord.on<ChatInputCommandInteractionCreateEvent> {
        // access control
        if (! allowStandardAccess(interaction)) return@on

        // parse command
        when (interaction.command.data.name.value) {
            cmd -> handleImageCommand(interaction, openAI)
            else -> return@on
        }
    }
}

/**
 * handle image command
 *
 * @param interaction
 * @param openAI
 */
private suspend fun handleImageCommand(interaction: ChatInputCommandInteraction, openAI: OpenAIUtils) {
    // acknowledge and defer command
    val response = interaction.deferPublicResponse()
    // get user
    val author = interaction.user
    // get input
    val size = interaction.command.strings[IMG_SIZE] ?: "256x256"
    val prompt = interaction.command.strings[IMG_PROMPT] ?: ""
    // generate image via DALL·E
    val imageResponse = openAI.createImage(author, size, prompt)
    // handle response
    if (imageResponse.success) {
        response.respond {
            embed {
                title = prompt
                field {
                    name = "author"
                    value = author.mention
                }
                field {
                    name = "size"
                    value = size
                }
                image = imageResponse.url
            }
        }
    } else {
        response.respond {
            content = "${author.mention} - failed to generate image with DALL·E\n```\n${imageResponse.url}\n```"
        }
    }
}