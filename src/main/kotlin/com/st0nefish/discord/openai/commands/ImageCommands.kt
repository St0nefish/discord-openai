package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.ImageExchange
import com.st0nefish.discord.openai.utils.CommandManager
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.embed

// constants
private const val IMG_MODEL = "model"
private const val IMG_QUALITY = "quality"
private const val IMG_STYLE = "style"
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
            string(IMG_PROMPT, "the prompt to send to DALL-E") { required = true }
        },
        { interaction -> handleImageCommand(interaction, openAI) })
    CommandManager.registerGlobalChatCommand(
        kord,
        "ask-dalle-adanced",
        "send a prompt to Dall-E with advanced options",
        {
            string(IMG_PROMPT, "the prompt to send to DALL-E") { required = true }
            string(IMG_MODEL, "Version of DALL-E to use") {
                required = false
                choice("dall-e-3", "DALL-E 3")
                choice("dall-e-2", "DALL-E 2")
            }
            string(IMG_QUALITY, "Quality of image") {
                required = false
                choice("standard", "Standard")
                choice("hd", "HD (more expensive)")
            }
            string(IMG_STYLE, "Style to use") {
                required = false
                choice("natural", "Natural - \"hyper-real and dramatic images\"")
                choice("vivid", "Vivid \"more natural, less hyper-real looking images\"")
            }
            string(IMG_SIZE, "Generated image size") {
                required = false
                choice("256x256", "256x256 (only v2)")
                choice("512x512", "512x512 (only v2)")
                choice("1024x1024", "1024x1024")
                choice("1024×1792", "1024×1792 (only v3)")
                choice("1792x1024", "1792x1024 (only v3)")
            }
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
    val response: DeferredPublicMessageInteractionResponseBehavior = interaction.deferPublicResponse()
    // get user
    val author: User = interaction.user
    // get input
    val model: String = interaction.command.strings[IMG_MODEL] ?: "dall-e-3"
    val quality: String = interaction.command.strings[IMG_QUALITY] ?: "standard"
    val style: String = interaction.command.strings[IMG_STYLE] ?: "natural"
    val size: String = interaction.command.strings[IMG_SIZE] ?: "1024x1024"
    val prompt: String = interaction.command.strings[IMG_PROMPT] ?: ""
    // generate image via DALL·E
    val imageResponse: ImageExchange = openAI.createImage(author, prompt, model, size, quality, style)
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
                    name = "model"
                    value = model
                }
                if (model == "dall-e-3") {
                    field {
                        name = "quality"
                        value = quality
                    }
                    field {
                        name = "style"
                        value = style
                    }
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
            content = "${author.mention} - failed to generate image with DALL·E\n```\n${imageResponse.exception}\n```"
        }
    }
}