package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Constants
import com.st0nefish.discord.openai.utils.OpenAIUtils
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.commands.commands

/**
 * Chat GPT commands
 */
@Suppress("unused")
fun chatGPTCommands() = commands("ChatGPT") {
    val openAI = OpenAIUtils.instance()

    /**
     * ask-gpt command that takes a prompt and feeds that to Chat GPT then outputs the response. when possible this will
     * reply with a single message but if the response is too long it will be broken up into multiple.
     */
    slash("ask-gpt", "ask chat GPT a question") {
        execute(AnyArg("Prompt", "prompt to feed to Chat-GPT")) {
            val prompt = args.first
            val delim = "\n\n"
            // immediately echo question privately
            respond("asking gpt: $prompt...")
            // get conversation object from OpenAIUtils
            val exchange = openAI.basicCompletion(author, prompt)
            // respond depending on status
            if (exchange.success) {
                // successful exchange response
                var msg = "${author.mention} asked: $prompt"
                // check if response is too long to post in a single message
                if ((msg.length + exchange.response.length) <= Constants.MAX_MESSAGE_LENGTH) {
                    // echoed question + response fits in one message
                    channel.createMessage(msg + exchange.response)
                } else {
                    // split response on delimiter
                    val split = exchange.response.split(delim)
                    // iterate to build messages just under discord's 2k character limit
                    for (part in split) {
                        // check if appending part would exceed limit
                        if ((msg.length + delim.length + part.length) > Constants.MAX_MESSAGE_LENGTH) {
                            channel.createMessage(msg)
                            msg = ""
                        }
                        // if not the first part append delimiter
                        if (msg.isNotBlank()) {
                            msg += delim
                        }
                        // append part
                        msg += part
                    }
                    // post final message if required
                    if (msg.isNotBlank()) channel.createMessage(msg)
                }
            } else {
                // failed exchange response
                channel.createMessage("${author.tag} - failed to get response from GPT:\n```${exchange.response}```")
            }
        }
    }
}