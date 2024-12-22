package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.Config
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

class CommandManager {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

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
                    if (AccessManager.allowStandardAccess(interaction)) {
                        action(interaction)
                    } else {
                        interaction.respondEphemeral {
                            content = "you are not permitted to use command [$name] in this chat"
                        }
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
            config: Config = Config.instance()
        ) {

            for (guildId in config.adminGuilds) {
                log.info("registering admin command [$name] in guild [$guildId]")
                // register guild command
                kord.createGuildChatInputCommand(Snowflake(guildId), name, description, command)
                // handle command body
                kord.on<GuildChatInputCommandInteractionCreateEvent> {
                    if (interaction.command.data.name.value == name) {
                        // handle action for this command for admin users
                        if (AccessManager.allowAdminAccess(interaction)) {
                            action(interaction)
                        } else {
                            interaction.respondEphemeral { content = "only admin users may use the [$name] command" }
                        }
                    }
                }
            }
        }

        /**
         * Registers a global user menu command and handles interactions for the registered command.
         *
         * @param kord The Kord instance used to interact with the Discord API.
         * @param name The name of the global user menu command to register.
         * @param action A suspendable lambda to be executed when the command is invoked.
         *               The lambda receives a `UserCommandInteraction` instance as its parameter.
         */
        suspend fun registerUserMenuCommand(
            kord: Kord, name: String, action: suspend (interaction: UserCommandInteraction) -> Unit = {}
        ) {
            kord.createGlobalUserCommand(name) {
                log.info("registered global user menu command $name")
            }
            // process menu command
            kord.on<UserCommandInteractionCreateEvent> {
                if (interaction.invokedCommandName == name) {
                    val user = interaction.user.asUser()
                    val target = interaction.target.asUser()
                    if (AccessManager.allowAdminAccess(interaction) || user.id == target.id) {
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
            val applicationId = kord.resources.applicationId

            // de-register global commands
            val globalCommands = kord.rest.interaction.getGlobalApplicationCommands(applicationId)
            log.info("de-registering ${globalCommands.size} global commands...")
            globalCommands.forEach { globalCommand ->
                log.info("de-registering global command [${globalCommand.name}]")
                kord.rest.interaction.deleteGlobalApplicationCommand(applicationId, globalCommand.id)
            }

            // de-register guild commands
            kord.guilds.toList().forEach { guild ->
                // get list of commands registered in this guild
                val guildCommands = kord.rest.interaction.getGuildApplicationCommands(applicationId, guild.id)
                log.info("de-registering ${guildCommands.size} commands in guild [${guild.data.name}]")
                guildCommands.forEach { command ->
                    log.info("de-registering guild command [${command.name}] from guild [${guild.data.name}]")
                    kord.rest.interaction.deleteGuildApplicationCommand(
                        applicationId, command.guildId.value!!, command.id
                    )
                }
            }
        }
    }
}