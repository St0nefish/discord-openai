package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.CommandManager
import com.st0nefish.discord.openai.utils.OpenAIUtils
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

// constants
private const val MAX_MESSAGE_LENGTH = 2000
private const val CHAT_DELIM = "\n\n"
private const val CHAT_PROMPT = "prompt"

/**
 * register chat commands
 *
 * @param kord {@link Kord} instance to interact with Discord
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
 */
suspend fun registerChatCommands(
    kord: Kord, openAI: OpenAIUtils = OpenAIUtils.instance()) {
    // register ask-gpt command
    CommandManager.registerGlobalChatCommand(
        kord,
        "ask-gpt",
        "send a prompt to GPT",
        { string(CHAT_PROMPT, "the prompt to send to Chat GPT") { required = true } },
        { interaction -> handleChatCommand(interaction, openAI) })
}

/**
 * handle chat command
 *
 * @param interaction {@link ChatInputCommandInteraction} details about the current command interaction
 * @param openAI {@link OpenAIUtils} instance to interact with OpenAI
 */
private suspend fun handleChatCommand(interaction: ChatInputCommandInteraction, openAI: OpenAIUtils) {
    // acknowledge command - defer response
    val response = interaction.deferPublicResponse()
    // get user
    val user = interaction.user
    // get prompt - required
    val prompt = interaction.command.strings[CHAT_PROMPT] ?: ""
    // ask gpt
    val exchange = openAI.getChatResponse(user, prompt)
    // handle response
    if (exchange.success) {
        // successful exchange response
        val promptMsg = "${user.mention}\n**Prompt:**\n$prompt"
        val responseMsg = "**Response:**\n"
        // check if response is too long to post in a single message
        if ((promptMsg.length + CHAT_DELIM.length + responseMsg.length + exchange.response.length) <= MAX_MESSAGE_LENGTH) {
            // echo question + full response in one message
            response.respond {
                content = promptMsg + CHAT_DELIM + responseMsg + exchange.response
            }
        } else {
            // response too long for one message
            // update initial response with the prompt
            response.respond {
                content = promptMsg
            }
            // output the rest in parts
            var msg = "${user.mention}\n$responseMsg"
            // split response on delimiter
            val parts = exchange.response.split(CHAT_DELIM)
            // iterate to build messages just under discord's character limit
            var first = true
            for (part in parts) {
                if (first) {
                    first = false
                } else {
                    // check if appending part would exceed limit
                    if ((msg.length + CHAT_DELIM.length + part.length) > MAX_MESSAGE_LENGTH) {
                        interaction.channel.createMessage(msg)
                        msg = ""
                    }
                    // if not the first part append delimiter
                    if (msg.isNotBlank()) {
                        msg += CHAT_DELIM
                    }
                }
                // append part
                msg += part
            }
            // post final message if required
            if (msg.trim().isNotBlank()) {
                interaction.channel.createMessage(msg)
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