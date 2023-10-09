package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.CommandManager
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed

// constants
private const val IMG_SIZE = "size"
private const val IMG_PROMPT = "prompt"

/**
 * register image commands
 *
 * @param kord {@link Kord} instance to interact with Discord
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
 */
suspend fun registerImageCommands(kord: Kord, openAI: OpenAIUtils = OpenAIUtils.instance()) {
    CommandManager.registerGlobalChatCommand(
        kord,
        "ask-dalle",
        "send a prompt to Dall-E",
        {
            string(IMG_SIZE, "Generated image size") {
                required = true
                choice("256x256", "256x256")
                choice("512x512", "512x512")
                choice("1024x1024", "1024x1024")
            }
            string(IMG_PROMPT, "the prompt to send to DALL-E") { required = true }
        },
        { interaction -> handleImageCommand(interaction, openAI) })
}

/**
 * handle image command
 *
 * @param interaction {@link ChatInputCommandInteraction} details about the current command interaction
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
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