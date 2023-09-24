package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.allowAdminAccess
import com.st0nefish.discord.openai.utils.allowStandardAccess
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.UserCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.commands.CommandManager")

/**
 * Register a global command
 *
 * @param kord Kord interface to interact with discord
 * @param name name of the command to register
 * @param description description of the command to register
 * @param command lambda to define command parameters
 * @param action lambda function to handle chat action
 */
suspend fun registerGlobalChatCommand(
    kord: Kord,
    name: String,
    description: String,
    command: ChatInputCreateBuilder.() -> Unit = {},
    action: suspend (interaction: ChatInputCommandInteraction) -> Unit = {},
) {
    log.info("registering global command [$name]")
    // register command so it shows up in slash commands
    kord.createGlobalChatInputCommand(name, description, command)
    // handle command body
    kord.on<ChatInputCommandInteractionCreateEvent> {
        if (interaction.command.data.name.value == name) {
            if (allowStandardAccess(interaction)) {
                action(interaction)
            } else {
                interaction.respondEphemeral { content = "you are not permitted to use command [$name] in this chat" }
            }
        }
    }
}

/**
 * Register a command only in the defined admin guilds
 *
 * @param kord Kord interface to interact with discord
 * @param name name of the command to register
 * @param description description of the command to register
 * @param command lambda to define command parameters
 * @param action lambda to define command action
 * @param config config object to get admin guilds from
 */
suspend fun registerAdminChatCommand(
    kord: Kord,
    name: String,
    description: String,
    command: ChatInputCreateBuilder.() -> Unit = {},
    action: suspend (interaction: ChatInputCommandInteraction) -> Unit = {},
    config: Config = Config.instance()) {

    for (guildId in config.adminGuilds) {
        log.info("registering admin command [$name] in guild [$guildId]")
        // register guild command
        kord.createGuildChatInputCommand(Snowflake(guildId), name, description, command)
        // handle command body
        kord.on<GuildChatInputCommandInteractionCreateEvent> {
            if (interaction.command.data.name.value == name) {
                // handle action for this command for admin users
                if (allowAdminAccess(interaction)) {
                    action(interaction)
                } else {
                    interaction.respondEphemeral { content = "only admin users may use the [$name] command" }
                }
            }
        }
    }
}

suspend fun registerUserMenuCommand(
    kord: Kord, name: String, action: suspend (interaction: UserCommandInteraction) -> Unit = {}) {
    kord.createGlobalUserCommand(name) {
        log.info("registered global user menu command $name")
    }
    // process menu command
    kord.on<UserCommandInteractionCreateEvent> {
        if (interaction.invokedCommandName == name) {
            val user = interaction.user.asUser()
            val target = interaction.target.asUser()
            if (allowAdminAccess(interaction) || user.id == target.id) {
                action(interaction)
            } else {
                interaction.respondEphemeral { content = "only admins are permitted to see usage data" }
            }
        }
    }
}


/**
 * Deregister all commands
 *
 * @param kord current Kord object
 */
suspend fun deregisterAllCommands(kord: Kord) {
    val applicationId = kord.resources.applicationId // deregister global commands
    val globalCommands = kord.rest.interaction.getGlobalApplicationCommands(applicationId)
    log.info("de-registering ${globalCommands.size} global commands...")
    globalCommands.forEach { globalCommand ->
        log.info("de-registering global command [${globalCommand.name}]")
        kord.rest.interaction.deleteGlobalApplicationCommand(applicationId, globalCommand.id)
    } // deregister guild commands for all guilds
    val guildCommands = kord.guilds.toList()
        .flatMap { guild -> kord.rest.interaction.getGuildApplicationCommands(applicationId, guild.id) }.toList()
    log.info("de-registering ${guildCommands.size} guild commands...")
    guildCommands.forEach { command ->
        log.info("de-registering guild [${command.guildId.value?.value}] command [${command.name}]")
        kord.rest.interaction.deleteGuildApplicationCommand(applicationId, command.guildId.value !!, command.id)
    }
}