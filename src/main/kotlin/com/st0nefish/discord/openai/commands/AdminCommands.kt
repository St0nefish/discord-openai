package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.APIUsage
import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.utils.AccessManager
import com.st0nefish.discord.openai.utils.CommandManager
import com.st0nefish.discord.openai.utils.DatabaseUtils
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.User
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.user

private const val PUBLIC_ARG = "public"
private const val PUBLIC_DESC = "show the response publicly - defaults to false"
private const val USER_ARG = "user"

/**
 * register admin commands
 *
 * @param kord {@link Kord} instance to interface with Discord
 * @param config {@link Config} instance with current configuration
 * @param database {@link DatabaseUtils} to interact with the usage database
 */
suspend fun registerAdminCommands(
    kord: Kord, config: Config = Config.instance(), database: DatabaseUtils = DatabaseUtils.instance()) {
    // register ping command
    CommandManager.registerGlobalChatCommand(
        kord,
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
 * @param kord {@link Kord} instance to interface with Discord
 * @param config {@link Config} instance with current configuration
 * @param database {@link DatabaseUtils} to interact with the usage database
 */
private suspend fun registerUsageCommands(kord: Kord, config: Config, database: DatabaseUtils) {
    // register get user usage chat command
    CommandManager.registerGlobalChatCommand(
        kord,
        "get-usage-user",
        "get a user's API usage data",
        {
            user(USER_ARG, "id of the user to get stats for - defaults to self") { required = false }
            boolean(PUBLIC_ARG, PUBLIC_DESC) { required = false }
        },
        { interaction ->
            run {
                // determine user to get stats for - defaults to command user
                val target =
                    interaction.command.users[USER_ARG] ?: interaction.user
                // respond as appropriate
                if (interaction.user == target || AccessManager.allowAdminAccess(
                        interaction,
                        config)) {
                    // anyone can get stats for themselves, admins can get stats for anyone
                    val msg = getUsageString(
                        target,
                        config,
                        database.getAPIUsage(target.id.value),
                        database.getAPIUsage(target.id.value, true))
                    if (interaction.command.booleans[PUBLIC_ARG] == true) {
                        interaction.respondPublic { content = msg }
                    } else {
                        interaction.respondEphemeral { content = msg }
                    }
                } else {
                    // access denied
                    interaction.respondEphemeral {
                        content =
                            "only admins are permitted to get usage stats for users other than themselves"
                    }
                }
            }
        })
    // register get total usage chat command
    CommandManager.registerGlobalChatCommand(
        kord,
        "get-usage-total",
        "get total API usage",
        { boolean(PUBLIC_ARG, PUBLIC_DESC) { required = false } },
        { interaction ->
            run {
                // only admins can get total usage data
                if (AccessManager.allowAdminAccess(interaction)) {
                    val msg = "```\n${database.getAPIUsage()}\n```"
                    if (interaction.command.booleans[PUBLIC_ARG] == true) {
                        interaction.respondPublic { content = msg }
                    } else {
                        interaction.respondEphemeral { content = msg }
                    }
                } else {
                    interaction.respondEphemeral {
                        content =
                            "only admins are permitted to use the get-usage-total command"
                    }
                }
            }
        })
    // register user usage menu command
    CommandManager.registerUserMenuCommand(kord, "menu-get-usage-user") { interaction ->
        run {
            // parse command target
            val target = interaction.target.asUser()
            // allow admins or those targeting self
            if (target == interaction.user || AccessManager.allowAdminAccess(interaction)) {
                interaction.respondEphemeral {
                    content = getUsageString(
                        target,
                        config,
                        database.getAPIUsage(target.id.value),
                        database.getAPIUsage(target.id.value, true))
                }
            } else {
                interaction.respondEphemeral {
                    content = "only admins are permitted to get usage stats for users other than themselves"
                }
            }
        }
    }
}

/**
 * register config commands
 *
 * @param kord {@link Kord} instance to interface with Discord
 * @param config {@link Config} instance with current configuration
 */
private suspend fun registerConfigCommands(kord: Kord, config: Config) {
    // register get-config command
    CommandManager.registerAdminChatCommand(
        kord,
        "get-config",
        "get the current bot config",
        {},
        { interaction -> interaction.respondEphemeral { content = "```$config```" } })
    CommandManager.registerAdminChatCommand(
        kord,
        "get-config-json",
        "get the current bot config as JSON",
        {},
        { interaction -> interaction.respondEphemeral { content = "```${config.toJson()}```" } })
    // register add admin user
    CommandManager.registerAdminChatCommand(
        kord,
        "add-admin-user",
        "add an admin user",
        { string(USER_ARG, "id of the user to add to the admins list") { required = true } },
        { interaction ->
            interaction.respondPublic {
                content =
                    "admin users: ${config.addAdminUser(interaction.command.strings[USER_ARG]!!.toULong())}"
            }
        })
    // register show admin users
    CommandManager.registerAdminChatCommand(
        kord,
        "show-admin-users",
        "show the list of admin users",
        {},
        { interaction -> interaction.respondPublic { content = "admin users: ${config.adminUsers}" } })
    // register add admin guild
    CommandManager.registerAdminChatCommand(
        kord,
        "add-admin-guild",
        "add a guild to the list of admin guilds",
        { string("guild", "id of the guild to add to the admin list") { required = true } },
        { interaction ->
            interaction.respondPublic {
                content = "admin guilds: ${config.addAdminGuild(interaction.command.strings["guild"]!!.toULong())}"
            }
        })
    // register add allowed channel
    CommandManager.registerAdminChatCommand(
        kord,
        "add-allow-channel",
        "add an allowed channel",
        { string("channel", "id of the channel to allow") { required = true } },
        { interaction ->
            interaction.respondPublic {
                content =
                    "allowed channels: ${config.addAllowChannel(interaction.command.strings["channel-id"]!!.toULong())}"
            }
        })
    // register show allowed channels
    CommandManager.registerAdminChatCommand(
        kord,
        "show-allow-channels",
        "show the list of allowed channels",
        {},
        { interaction -> interaction.respondPublic { content = "allowed channels: ${config.allowChannels}" } })
    // register add allowed user
    CommandManager.registerAdminChatCommand(
        kord,
        "add-allow-user",
        "add a user to the list allowed to use the bot via PM",
        { string(USER_ARG, "id of the user to allow") { required = true } },
        { interaction ->
            interaction.respondPublic {
                content = "allowed users: ${config.addAllowUsers(interaction.command.strings[USER_ARG]!!.toULong())}"
            }
        })
    // register show allowed users
    CommandManager.registerAdminChatCommand(
        kord,
        "show-allow-users",
        "show the list of users allowed to use the bot via PM",
        {},
        { interaction -> interaction.respondPublic { content = "allowed users: ${config.allowUsers}" } })
    // register add unlimited user
    CommandManager.registerAdminChatCommand(
        kord,
        "add-unlimited-user",
        "add a user to the list of users exempt from the timed usage limits",
        { string(USER_ARG, "id of the user to allow unlimited use") { required = true } },
        { interaction ->
            interaction.respondPublic {
                content =
                    "unlimited users: ${config.addUnlimitedUser(interaction.command.strings[USER_ARG]!!.toULong())}"
            }
        })
    // register show unlimited users
    CommandManager.registerAdminChatCommand(
        kord,
        "show-unlimited-users",
        "show the list of users exempt from the timed usage limits",
        {},
        { interaction -> interaction.respondPublic { content = "unlimited users: ${config.unlimitedUsers}" } })
    // register set usage limit
    CommandManager.registerAdminChatCommand(
        kord, "set-usage-limit",
        "set the usage limit per user per interval",
        {
            number("value", "cost (in dollars) per user per usage interval") { required = true }
            number("interval", "interval (in hours) over which the usage limit is tracked") { required = true }
        },
        { interaction ->
            interaction.respondPublic {
                content = "usage limit is ${
                    config.setUsageLimit(
                        interaction.command.numbers["value"]!!,
                        interaction.command.numbers["interval"]!!.toInt())
                }"
            }
        })
}

/**
 * register commands to get recent interactions
 *
 * @param kord {@link Kord} instance to interface with Discord
 * @param database {@link DatabaseUtils} to interact with the usage database
 */
private suspend fun registerGetLastCommands(kord: Kord, database: DatabaseUtils) {
    // register get-last-message command
    CommandManager.registerAdminChatCommand(
        kord,
        "get-last-chat",
        "get the last chat exchange for the specified user",
        {
            user(USER_ARG, "id of the user to get the last chat exchange for") { required = false }
            boolean(PUBLIC_ARG, PUBLIC_DESC) { required = false }
        },
        { interaction ->
            run {
                val user = interaction.command.users[USER_ARG] ?: interaction.user
                val response = "last exchange for ${user.tag}:\n```\n${
                    database.getLastChatExchange(user.id.value).toString()
                }\n```".trimIndent()
                if (interaction.command.booleans[PUBLIC_ARG] == true) {
                    interaction.respondPublic { content = response }
                } else {
                    interaction.respondEphemeral { content = response }
                }
            }
        })
    // register get-last-image command
    CommandManager.registerAdminChatCommand(
        kord,
        "get-last-image",
        "get the last image exchange for the specified user",
        {
            user(USER_ARG, "id of the user to get the last image exchange for") { required = false }
            boolean(PUBLIC_ARG, PUBLIC_DESC) { required = false }
        },
        { interaction ->
            run {
                val user = interaction.command.users[USER_ARG] ?: interaction.user
                val response = "last image for ${user.tag}:\n```\n${
                    database.getLastImage(user.id.value).toString()
                }\n```".trimIndent()
                if (interaction.command.booleans[PUBLIC_ARG] == true) {
                    interaction.respondPublic { content = response }
                } else {
                    interaction.respondEphemeral { content = response }
                }
            }
        })
}

/**
 * return a nicely formatted user string for both total and timed api usage stats
 *
 * @param user user to show stats for
 * @param config config object
 * @param totalUsage APIUsage object with lifetime data
 * @param timedUsage APIUsage object with timed data
 * @return
 */
fun getUsageString(user: User, config: Config, totalUsage: APIUsage, timedUsage: APIUsage): String {
    return "API usage for ${user.tag}:\n```\nTotal:\n${
        totalUsage.toString().replaceIndent("    ")
    }\nLast ${config.usageCostInterval} hours:\n${
        timedUsage.toString().replaceIndent("    ")
    }\n```".trimIndent()
}