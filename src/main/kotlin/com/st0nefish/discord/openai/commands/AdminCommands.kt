package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.DatabaseUtils
import com.st0nefish.discord.openai.utils.cmdByAdmin
import com.st0nefish.discord.openai.utils.getUsageString
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.st0nefish.discord.openai.registerAdminCommands")

/**
 * register admin commands
 *
 * @param kord
 * @param config
 * @param database
 */
suspend fun registerAdminCommands(
    kord: Kord, config: Config = Config.instance(), database: DatabaseUtils = DatabaseUtils.instance()) {
    //////// register global admin commands ////////
    log.info("registering admin global commands")

    // register ping command
    registerPingCommand(kord)

    // register get user usage command
    registerUsageCommands(kord, config, database)

    //////// register admin guild commands ////////
    if (config.adminGuild != null) {
        log.info("registering admin guild commands for [${config.adminGuild}]")

        // register config commands
        registerConfigCommands(kord, config.adminGuild, config)

        // register get last commands
        registerGetLastCommands(kord, config.adminGuild, database)
    }
}

/**
 * register a ping command
 *
 * @param kord
 */
private suspend fun registerPingCommand(kord: Kord) {
    val cmd = "ping"
    kord.createGlobalChatInputCommand(cmd, "a ping test command") {
        log.info("registered global chat command: $name")
    }
    kord.on<ChatInputCommandInteractionCreateEvent> {
        if (interaction.command.data.name.value != cmd) return@on
        interaction.respondPublic {
            content = "${interaction.user.mention} pong"
        }
    }
}

/**
 * register usage commands
 *
 * @param kord
 * @param config
 * @param database
 */
private suspend fun registerUsageCommands(kord: Kord, config: Config, database: DatabaseUtils) {
    // register get user usage chat command
    val userChatCmd = "get-usage"
    kord.createGlobalChatInputCommand(userChatCmd, "get user's API usage data") {
        user("user", "user to get stats for") { required = false }
        boolean("public", "show response publicly") { required = false }
        log.info("registered global chat command: $name")
    }
    // register get total usage chat command
    val totalChatCmd = "get-usage-total"
    kord.createGlobalChatInputCommand(totalChatCmd, "get total API usage") {
        boolean("public", "show response publicly") { required = false }
        log.info("registered global chat command: $name")
    }

    // handle chat commands
    kord.on<ChatInputCommandInteractionCreateEvent> {
        // switch on command name
        when (interaction.command.data.name.value) {
            // usage stats for a specific user
            userChatCmd -> {
                // parse user requesting stats and target of stats
                val user = interaction.user
                val target = interaction.command.users["user"] ?: interaction.user
                // admins can get stats for anyone, anyone can get stats for themselves
                if (cmdByAdmin(interaction, config) || user.id == target.id) {
                    val public = interaction.command.booleans["public"] ?: false
                    val msg = getUsageString(
                        target,
                        config,
                        database.getAPIUsage(target.id.value),
                        database.getAPIUsage(target.id.value, true))
                    if (public) {
                        interaction.respondPublic { content = msg }
                    } else {
                        interaction.respondEphemeral { content = msg }
                    }
                } else {
                    interaction.respondEphemeral { content = "command not permitted by non-admin users" }
                }
            }
            // total usage stats
            totalChatCmd -> {
                // access control - only admins
                if (cmdByAdmin(interaction, config)) {
                    // respond with message
                    val msg = "```\n${database.getAPIUsage()}\n```"
                    if (interaction.command.booleans["public"] == true) {
                        interaction.respondPublic { content = msg }
                    } else {
                        interaction.respondEphemeral { content = msg }
                    }
                } else {
                    interaction.respondEphemeral { content = "command not permitted by non-admin users" }
                }
            }
            // otherwise do nothing
            else -> return@on
        }
    }

    // register user usage menu command
    val userMenuCmd = "menu-get-usage"
    kord.createGlobalUserCommand(userMenuCmd) {
        log.info("registered global menu command $name")
    }
    // process menu command
    kord.on<UserCommandInteractionCreateEvent> {
        if (interaction.invokedCommandName == userMenuCmd) {
            // parse command user and target
            val user = interaction.user.asUser()
            val target = interaction.target.asUser()
            // allow admins or those targeting self
            if (cmdByAdmin(interaction, config) || user.id == target.id) {
                interaction.respondEphemeral {
                    content = getUsageString(
                        target,
                        config,
                        database.getAPIUsage(target.id.value),
                        database.getAPIUsage(target.id.value, true))
                }
            } else {
                interaction.respondEphemeral { content = "command not permitted by non-admin users" }
            }
        } else return@on
    }
}

/**
 * register config commands
 *
 * @param kord
 * @param adminGuild
 * @param config
 */
private suspend fun registerConfigCommands(kord: Kord, adminGuild: Snowflake, config: Config) {
    // register get-config command
    kord.createGuildChatInputCommand(adminGuild, "get-config", "get bot config") {
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register add allowed channel
    kord.createGuildChatInputCommand(adminGuild, "add-allow-channel", "add an allowed channel") {
        string("channel-id", "id of the channel to allow") { required = true }
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register get allowed channels
    kord.createGuildChatInputCommand(adminGuild, "get-allow-channels", "show allowed channel") {
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register add allowed user
    kord.createGuildChatInputCommand(adminGuild, "add-allow-user", "add an allowed user") {
        string("user-id", "id of the user to allow") { required = true }
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register get allowed users
    kord.createGuildChatInputCommand(adminGuild, "get-allow-users", "show allowed users") {
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register add unlimited user
    kord.createGuildChatInputCommand(adminGuild, "add-unlimited-user", "add an unlimited user") {
        string("user-id", "id of the user to allow") { required = true }
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register show unlimited users
    kord.createGuildChatInputCommand(adminGuild, "get-unlimited-users", "add an unlimited user") {
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }

    //// process chat commands ////
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        // access control
        if (! cmdByAdmin(interaction, config)) {
            interaction.respondEphemeral { content = "access denied" }
            return@on
        }

        // handle commands
        when (interaction.command.data.name.value) {
            // get configuration object
            "get-config" -> {
                interaction.respondEphemeral { content = "```$config```" }
            }
            // add to the allowed channel list
            "add-allow-channel" -> {
                val channelId = interaction.command.strings["channel-id"] ?: ""
                config.addAllowChannel(Snowflake(channelId))
                interaction.respondPublic { content = "allowed channels: ${config.allowChannels}" }
            }
            // output the allowed channel list
            "get-allow-channels" -> {
                interaction.respondPublic { content = "allowed channels: ${config.allowChannels}" }
            }
            // add an allowed user
            "add-allow-user" -> {
                val channelId = interaction.command.strings["user-id"] ?: ""
                config.addAllowUsers(Snowflake(channelId))
                interaction.respondPublic { content = "allowed users: ${config.allowUsers}" }
            }
            // get the allowed user list
            "get-allow-users" -> {
                interaction.respondPublic { content = "allowed users: ${config.allowUsers}" }
            }
            // add an unlimited user
            "add-unlimited-user" -> {
                val channelId = interaction.command.strings["user-id"] ?: ""
                config.addUnlimitedUser(Snowflake(channelId))
                interaction.respondPublic { content = "unlimited users: ${config.unlimitedUsers}" }
            }
            // output the unlimited user list
            "get-unlimited-users" -> {
                interaction.respondPublic { content = "unlimited users: ${config.unlimitedUsers}" }
            }
            // otherwise do nothing
            else -> return@on
        }
    }
}

/**
 * register commands to get recent interactions
 *
 * @param kord
 * @param adminGuild
 * @param database
 */
private suspend fun registerGetLastCommands(
    kord: Kord, adminGuild: Snowflake, database: DatabaseUtils) {
    // register get-last-message command
    kord.createGuildChatInputCommand(adminGuild, "get-last-chat", "get last chat exchange") {
        user("user", "user to get last message for") { required = false }
        boolean("public", "should the response be public") { required = false }
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }
    // register get-last-image command
    kord.createGuildChatInputCommand(adminGuild, "get-last-image", "get last image exchange") {
        user("user", "user to get last message for") { required = false }
        boolean("public", "should the response be public") { required = false }
        log.info("registered guild [${adminGuild.value}] chat command $name")
    }

    // handle chat interaction
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        // access control
        if (! cmdByAdmin(interaction)) {
            interaction.respondEphemeral { content = "command not permitted by non-admin users" }
            return@on
        }

        // handle by command
        when (interaction.command.data.name.value) {
            // get most recent chat interaction for specified user
            "get-last-chat" -> {
                val user = interaction.command.users["user"] ?: interaction.user

                val response = "last exchange for ${user.tag}:\n```\n${
                    database.getLastChatExchange(user.id.value).toString()
                }\n```".trimIndent()

                if (interaction.command.booleans["public"] == true) {
                    interaction.respondPublic { content = response }
                } else {
                    interaction.respondEphemeral { content = response }
                }
            }
            // get most recent image interaction for specified user
            "get-last-image" -> {
                val user = interaction.command.users["user"] ?: interaction.user
                val response = "last image for ${user.tag}:\n```\n${
                    database.getLastImage(user.id.value).toString()
                }\n```".trimIndent()
                if (interaction.command.booleans["public"] == true) {
                    interaction.respondPublic { content = response }
                } else {
                    interaction.respondEphemeral { content = response }
                }
            }
            // otherwise do nothing
            else -> return@on
        }
    }
}