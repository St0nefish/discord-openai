package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.ChatExchange
import com.st0nefish.discord.openai.utils.CommandManager
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.DeferredPublicMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string
import org.slf4j.LoggerFactory

// constants
private const val MAX_MESSAGE_LENGTH = 2000
private const val CHAT_DELIM = "\n\n"
private const val INPUT_PROMPT = "prompt"
private const val INPUT_MODEL = "model"
private const val DEFAULT_MODEL = "gpt-4o-mini"

// logger
private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.ChatCommands")

/**
 * register chat commands
 *
 * @param kord {@link Kord} instance to interact with Discord
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
 */
suspend fun registerChatCommands(
    kord: Kord, openAI: OpenAIUtils = OpenAIUtils.instance()
) {
    // register ask-gpt command
    CommandManager.registerGlobalChatCommand(
        kord,
        "ask-gpt",
        "send a prompt to GPT",
        { string(INPUT_PROMPT, "the prompt to send to Chat GPT") { required = true } },
        { interaction: ChatInputCommandInteraction -> handleChatCommand(interaction, openAI) }
    )
    // register ask-gpt-model command
    CommandManager.registerGlobalChatCommand(
        kord,
        "ask-gpt-advanced",
        "send a prompt to a specific GPT model",
        {
            string(INPUT_MODEL, "GPT model to use") {
                required = true
                choice("GPT-4o mini", "gpt-4o-mini")
                choice("GPT 4o", "gpt-4o")
                choice("GPT 3.5 turbo", "gpt-3.5-turbo-1106")
                choice("o1-mini", "o1-mini")
            }
            string(INPUT_PROMPT, "the prompt to send to Chat GPT") { required = true }
        },
        { interaction: ChatInputCommandInteraction -> handleChatCommand(interaction, openAI) }
    )
}

/**
 * handle chat command
 *
 * @param interaction {@link ChatInputCommandInteraction} details about the current command interaction
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
 */
private suspend fun handleChatCommand(interaction: ChatInputCommandInteraction, openAI: OpenAIUtils) {
    // acknowledge command - defer response
    val response: DeferredPublicMessageInteractionResponseBehavior = interaction.deferPublicResponse()
    // get user
    val user: User = interaction.user
    // get model name - required but defaulted
    val model: String = interaction.command.strings[INPUT_MODEL] ?: DEFAULT_MODEL
    // get prompt - required
    val prompt: String = interaction.command.strings[INPUT_PROMPT] ?: ""
    // ask gpt
    val exchange: ChatExchange = openAI.getChatResponse(user, prompt, model)
    // handle response
    if (exchange.success) {
        // successful exchange response
        val responseHeader: String = """
           ${user.mention} 
           **Prompt:**
           $prompt
           
           **Model:**
           $model
        """.trimIndent()
        val singlePartResponseHeader: String = "**Response:**\n"
        val multiPartResponseHeader: String = "**Response xx of yy:**\n"
        // check if response is too long to post in a single message
        if (
            (responseHeader.length
                    + CHAT_DELIM.length
                    + singlePartResponseHeader.length
                    + exchange.response.length)
            <=
            MAX_MESSAGE_LENGTH
        ) {
            // respond with user tag + question + model + answer
            response.respond {
                content = responseHeader + CHAT_DELIM + singlePartResponseHeader + exchange.response
            }
        } else {
            // response too long for one message - break it up into parts
            // update initial response with the prompt
            // split response on delimiter (two line feeds) for clean breaks
            val parts: List<String> = exchange.response.split(CHAT_DELIM)
            // calculate header sizes for determining parts
            val nPartHeaderSize: Int = user.tag.length + "\n".length + multiPartResponseHeader.length
            // start iterating parts to built list of output messages
            val responses: MutableList<String> = mutableListOf()
            var currentResponse: String = ""
            for (part: String in parts) {
                if ((nPartHeaderSize + currentResponse.length + CHAT_DELIM.length + part.length)
                    <= MAX_MESSAGE_LENGTH
                ) {
                    // can include this part in response with header
                    // append delimiter if we have existing parts
                    if (currentResponse.isNotEmpty()) {
                        currentResponse += CHAT_DELIM
                    }
                    // append this part
                    currentResponse += part
                } else {
                    // next part would make this too long - push current part to list and reset to this part
                    responses.add(currentResponse)
                    currentResponse = part
                }
            }
            // handle remaining parts if present
            if (currentResponse.isNotEmpty()) {
                responses.add(currentResponse)
            }
            log.info("split response into [${responses.size}] parts")
            // now respond - first output the response header
            response.respond {
                content = responseHeader + CHAT_DELIM + "**Response:**\nin ${responses.size} parts below"
            }
            // then iterate responses to reply
            responses.forEachIndexed { index: Int, responsePart: String ->
                // subsequent parts use short version
                interaction.channel.createMessage(
                    "${user.mention}\n**Response ${index + 1} of ${responses.size}:**\n" + responsePart.trim()
                )
            }
        }
    } else {
        // failed exchange response
        response.respond {
            content =
                "${interaction.user.mention} failed to get a response for prompt: $prompt\n\n```${exchange.response}```"
        }
    }
}