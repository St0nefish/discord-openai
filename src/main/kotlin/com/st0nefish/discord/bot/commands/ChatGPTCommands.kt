package com.st0nefish.discord.bot.commands

import com.st0nefish.discord.bot.data.Config
import com.st0nefish.discord.bot.data.Constants
import com.st0nefish.discord.bot.services.OpenAIUtils
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.commands.commands
import org.slf4j.LoggerFactory

fun chatGPTCommands(config: Config) = commands("ChatGPT") {
    val log = LoggerFactory.getLogger(this.javaClass.name)
    val openAI = OpenAIUtils(config)

    slash("ask-gpt", "ask chat GPT a question") {
        execute(AnyArg("Prompt", "prompt to feed to Chat-GPT")) {
            val prompt = args.first
            val delim = "\n\n"
            // immediately echo question privately
            respond("asking gpt: $prompt...")
            // log question
            println("${author.tag} asked: $prompt")
            log.info("${author.tag} asked: $prompt")
            // ask GPT
            var msg = "${author.username} asked: $prompt"
            val response = openAI.basicCompletion(author.id.value, prompt)
            // check if response is too long to post in a single message
            if ((msg.length + response.length) <= Constants.MAX_MESSAGE_LENGTH) {
                // echoed question + response fits in one message
                channel.createMessage(msg + response)
            } else {
                // split response on delimiter
                val split = response.split(delim)
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
                // log response
                println("${author.tag} asked \"$prompt\" and got response:$delim$response")
                log.info("${author.tag} asked \"$prompt\" and got response:$delim$response")
            }
        }
    }
}