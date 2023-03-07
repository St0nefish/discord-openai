package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.Constants
import com.st0nefish.discord.openai.utils.OpenAIUtils
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.commands.commands
import org.slf4j.LoggerFactory

@Suppress("unused")
fun chatGPTCommands(config: Config) = commands("ChatGPT") {
    val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.commands.ChatGPTCommands")
    val openAI = OpenAIUtils(config)

    slash("ask-gpt", "ask chat GPT a question") {
        execute(AnyArg("Prompt", "prompt to feed to Chat-GPT")) {
            val prompt = args.first
            val delim = "\n\n"
            // immediately echo question privately
            respond("asking gpt: $prompt...")
            // log question
            log.info("${author.tag} asked: $prompt")
            // ask GPT
            var msg = "${author.mention} asked: $prompt"
            val response: String
            try {
                response = openAI.basicCompletion(author, prompt)
            } catch (e: Exception) {
                log.error(
                    "exception getting GPT completion%nAuthor: %s:%nPrompt:%s%nException:%n%s".format(
                        author.tag, prompt, e.stackTraceToString()
                    )
                )
                channel.createMessage(
                    "%s - failed to get completion from GPT%nPrompt: %s%nException:%n```%s```".format(
                        author.mention, prompt, e.stackTraceToString()
                    )
                )
                return@execute
            }
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
            }
            // log response
            log.info(
                "GPT response:%nAuthor: %s%nPrompt: %s%nResponse: %s".format(
                    author.tag, prompt, response.trim()
                )
            )
        }
    }
}