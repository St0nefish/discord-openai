package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.DatabaseUtils
import com.st0nefish.discord.openai.utils.allowAdminAccess
import com.st0nefish.discord.openai.utils.getUsageString
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

/**
 * register admin commands
 *
 * @param kord
 * @param config
 * @param database
 */
suspend fun registerAdminCommands(
    kord: Kord, config: Config = Config.instance(), database: DatabaseUtils = DatabaseUtils.instance()) {
    // register ping command
    registerGlobalChatCommand(kord,
        "ping",
        "ping?",
        {},
        { it.respondPublic { content = "${it.user.mention} - PONG!" } })

    // register get user usage command
    registerUsageCommands(kord, config, database)

    // register config commands
    registerConfigCommands(kord, config)

    // register get last commands
    registerGetLastCommands(kord, database)
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
    registerGlobalChatCommand(kord, "get-usage", "get a user's API usage data", {
        user("user", "user to get stats for") { required = false }
        boolean("public", "show response publicly") { required = false }
    }, { interaction ->
        run {
            if (allowAdminAccess(interaction, config)) {
                // parse user requesting stats and target of stats
                val target = interaction.command.users["user"] ?: interaction.user
                // admins can get stats for anyone, anyone can get stats for themselves
                val public = interaction.command.booleans["public"] ?: false
                val msg = getUsageString(
                    target, config, database.getAPIUsage(target.id.value), database.getAPIUsage(target.id.value, true))
                if (public) {
                    interaction.respondPublic { content = msg }
                } else {
                    interaction.respondEphemeral { content = msg }
                }
            } else {
                interaction.respondEphemeral {
                    content = "only admins are permitted to use the get-usage command"
                }
            }
        }
    })
    // register get total usage chat command
    registerGlobalChatCommand(kord, "get-usage-total", "get total API usage", {
        boolean("public", "show response publicly") { required = false }
    }, { interaction ->
        run {
            if (allowAdminAccess(interaction)) {
                val msg = "```\n${database.getAPIUsage()}\n```"
                if (interaction.command.booleans["public"] == true) {
                    interaction.respondPublic { content = msg }
                } else {
                    interaction.respondEphemeral { content = msg }
                }
            } else {
                interaction.respondEphemeral {
                    content = "only admins are permitted to use the get-usage-total command"
                }
            }
        }
    })

    // register user usage menu command
    registerUserMenuCommand(kord, "menu-get-usage") { interaction ->
        run {
            // parse command target
            val target = interaction.target.asUser()
            // allow admins or those targeting self
            interaction.respondEphemeral {
                content = getUsageString(
                    target, config, database.getAPIUsage(target.id.value), database.getAPIUsage(target.id.value, true))
            }
        }
    }
}

/**
 * register config commands
 *
 * @param kord
 * @param config
 */
private suspend fun registerConfigCommands(kord: Kord, config: Config) {
    // register get-config command
    registerAdminChatCommand(kord,
        "get-config",
        "get bot config",
        {},
        { interaction -> interaction.respondEphemeral { content = "```$config```" } })
    // register add allowed channel
    registerAdminChatCommand(kord,
        "add-allow-channel",
        "add an allowed channel",
        { string("channel-id", "id of the channel to allow") { required = true } },
        { interaction ->
            run {
                val channelId = interaction.command.strings["channel-id"] ?: ""
                if (channelId.isNotBlank()) {
                    config.addAllowChannel(Snowflake(channelId))
                    interaction.respondPublic { content = "allowed channels: ${config.allowChannels}" }
                }
            }
        })
    // register get allowed channels
    registerAdminChatCommand(kord,
        "get-allow-channels",
        "list allowed channels",
        {},
        { interaction -> interaction.respondPublic { content = "allowed channels: ${config.allowChannels}" } })
    // register add allowed user
    registerAdminChatCommand(kord,
        "add-allow-user",
        "add an allowed user",
        { string("user-id", "id of the user to allow") { required = true } },
        { interaction ->
            run {
                val userId = interaction.command.strings["user-id"] ?: ""
                if (userId.isNotBlank()) {
                    config.addAllowUsers(Snowflake(userId))
                    interaction.respondPublic { content = "allowed users: ${config.allowUsers}" }
                }
            }
        })
    // register get allowed users
    registerAdminChatCommand(kord,
        "get-allow-users",
        "list allowed users",
        {},
        { interaction -> interaction.respondPublic { content = "allowed users: ${config.allowUsers}" } })
    // register add unlimited user
    registerAdminChatCommand(kord,
        "add-unlimited-user",
        "add an unlimited user",
        { string("user-id", "id of the user to allow") { required = true } },
        { interaction ->
            run {
                val channelId = interaction.command.strings["user-id"] ?: ""
                config.addUnlimitedUser(Snowflake(channelId))
                interaction.respondPublic { content = "unlimited users: ${config.unlimitedUsers}" }
            }
        })
    // register show unlimited users
    registerAdminChatCommand(kord,
        "get-unlimited-users",
        "add an unlimited user",
        {},
        { interaction -> interaction.respondPublic { content = "unlimited users: ${config.unlimitedUsers}" } })
}

/**
 * register commands to get recent interactions
 *
 * @param kord
 * @param database
 */
private suspend fun registerGetLastCommands(kord: Kord, database: DatabaseUtils) {
    // register get-last-message command
    registerAdminChatCommand(kord, "get-last-chat", "get last chat exchange for a user", {
        user("user", "user to get last message for") { required = false }
        boolean("public", "should the response be public") { required = false }
    }, { interaction ->
        run {
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
    })
    // register get-last-image command
    registerAdminChatCommand(kord, "get-last-image", "get the last image exchange for a user", {
        user("user", "user to get last message for") { required = false }
        boolean("public", "should the response be public") { required = false }
    }, { interaction ->
        run {
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
    })
}