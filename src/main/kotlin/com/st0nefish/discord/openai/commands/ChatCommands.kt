package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.utils.OpenAIUtils
import com.st0nefish.discord.openai.utils.allowStandardAccess
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import org.slf4j.LoggerFactory

// constants
private const val MAX_MESSAGE_LENGTH = 2000
private const val CHAT_DELIM = "\n\n"
private const val CHAT_PROMPT = "prompt"

// logger
private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.registerChatCommands")

/**
 * register chat commands
 *
 * @param kord
 * @param openAI
 */
suspend fun registerChatCommands(
    kord: Kord, openAI: OpenAIUtils = OpenAIUtils.instance()) { // register ask-gpt command
    // define command key
    val cmd = "ask-gpt"

    // register chat command
    kord.createGlobalChatInputCommand(cmd, "send a prompt to chat GPT") {
        string(CHAT_PROMPT, "the prompt to send to Chat GPT") {
            required = true
        }
        log.info("registered command: $name")
    }

    // handle chat command
    kord.on<ChatInputCommandInteractionCreateEvent> {
        // access control
        if (! allowStandardAccess(interaction)) return@on

        // parse command
        when (interaction.command.data.name.value) {
            cmd -> handleChatCommand(interaction, openAI)
            else -> return@on
        }
    }
}

/**
 * handle chat command
 *
 * @param interaction
 * @param openAI
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