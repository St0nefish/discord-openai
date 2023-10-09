package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.CommandManager
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.string

/**
 * Register general commands
 *
 * @param kord {@link Kord} instance to interface with Discord
 * @param config {@link Config} instance with current configuration
 */
suspend fun registerGeneralCommands(kord: Kord, config: Config = Config.instance()) {
    // register the info command
    CommandManager.registerGlobalChatCommand(
        kord,
        "info",
        "get general information about this bot",
        {},
        { interaction -> handleInfoCommand(interaction, kord, config) })
    // register the help command
    CommandManager.registerGlobalChatCommand(
        kord,
        "help",
        "get help for this bot",
        { string("command", "command to get help for") { required = false } },
        { interaction -> handleHelpCommand(interaction) })
}

/**
 * Handle the info command
 *
 * @param interaction {@link ChatInputCommandInteraction} details about the current command interaction
 * @param kord {@link Kord} instance to interface with Discord
 * @param config {@link Config} instance with current configuration
 */
private suspend fun handleInfoCommand(interaction: ChatInputCommandInteraction, kord: Kord, config: Config) {
    interaction.respondEphemeral {
        content = """
            Discord bot for working with OpenAI
            
            Project: https://github.com/St0nefish/discord-openai
            Author:  St0nefish
            Admins:  ${config.adminUsers.mapNotNull { kord.getUser(Snowflake(it))?.tag }.joinToString(", ")}
        """.trimIndent()
    }
}

/**
 * Handle the help command
 *
 * @param interaction {@link ChatInputCommandInteraction} details about the current command interaction
 */
private suspend fun handleHelpCommand(interaction: ChatInputCommandInteraction) {
    // help for a specific command
    val msg: String
    when (interaction.command.strings["command"]) {
        "ask-gpt" -> msg = """>
            ask-gpt prompt
                Send a prompt to ChatGPT. 
                
                Args:
                  prompt: The prompt to send to ChatGPT
                
                The bot will appear to be typing while the message is processed. Once the response is received it will be output publicly to the channel in which the command was used. The message will tag the user who executed the command and echo the original input prompt.
                If the prompt violates OpenAI's policies then an error may be displayed instead.
            """.trimIndent()

        "ask-dalle" -> msg = """>
            ask-dalle prompt
                Send a prompt to DALL-E
                
                Args:
                  image-size: The size of the image to generate. larger images cost more to generate. Options are: 256x256, 512x512, 1024x1024 and all sizes are in pixels.
                  prompt: The prompt to send to DALL-E to generate the image with
                 
                The bot will appear to be typing while the image is processed. Once the response is received it will be output publicly to the channel in which the command was used. The message will tag the user who executed the command and display the requested size and input prompt.
                If the prompt violates OpenAI's policies then an error may be displayed instead.
            """.trimIndent()

        "get-usage" -> msg = """>
            get-usage [user] [public]
                Get the usage stats for the specified user.
                
                Args:
                  user (optional): Defaults to self. The ID of the user to get usage data for. Only admin users can request usage data for users other than themselves. 
                  public (optional): Defaults to false. Should the response be sent privately to the user of the command (public=false), or publicly to the channel in which the command was used (public=true).
            """.trimIndent()

        "get-usage-total" -> msg = """>
            get-usage-total
                Get the total usage stats for this bot instance. Only admins may use this command.
                
                Args:
                  public (optional): Defaults to false. Should the response be sent privately to the user of the command (public=false), or publicly to the channel in which the command was used (public=true).
            """.trimIndent()

        "info" -> msg = """>
            info
                Get general info about this bot. 
            """.trimIndent()

        "help" -> msg = """>
            help
                Get help using this bot. 
                
                Args:
                  command (optional): The specific command to get help text for. If not passed outputs some general help.
            """.trimIndent()

        else -> msg = """>
            Discord-OpenAI
                A Discord bot for working with OpenAI
                
                Commands: 
                - ask-gpt [prompt] - send a prompt to ChatGPT
                - ask-dalle [image-size] [prompt] - generate an image with DALL-E of the specified size using the input prompt
                - get-usage [user]? [public]? - get usage stats for the specified user
                - get-usage-total - get total usage stats for this bot instance
                - info - get general info about this bot
                - help - output this help text
            """.trimIndent()
    }
    interaction.respondEphemeral { content = msg }
}