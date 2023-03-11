package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.Constants
import com.st0nefish.discord.openai.utils.DatabaseUtils
import com.st0nefish.discord.openai.utils.StringFormatUtils
import dev.kord.common.entity.Snowflake
import me.jakejmattson.discordkt.arguments.AnyArg
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.dsl.edit
import java.util.stream.Collectors

/**
 * create administrator-only commands
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun adminCommands(config: Config) = commands(Constants.ADMIN_CATEGORY) {
    val database = DatabaseUtils.instance()
    val publicArg = BooleanArg("public", "respond publicly (if true) or privately (if false)").optional(false)

    /**
     * admin-get-config command to output the current Bot configuration via private reply
     */
    slash("admin-get-config", "admin command to get bot configuration") {
        execute {
            respond("```${config.asString(discord.kord)}```")
        }
    }

    /**
     * admin-user-usage <User> command to get the current API usage stats for the specified user. an optional second
     * parameter can be passed to output the stats publicly (if true) or only to the admin (if false)
     */
    slash("admin-user-usage", "get user OpenAPI stats") {
        execute(UserArg("user", "user to get usage data for").optional { it.author }, publicArg) {
            val (user, public) = args
            val response = StringFormatUtils.getUsageString(
                user, config, database.getAPIUsage(user.id.value), database.getAPIUsage(user.id.value, true)
            )
            if (public) {
                respondPublic(response)
            } else {
                respond(response)
            }
        }
    }

    /**
     * admin-total-usage command to get the current total API usage stats across all users. an optional parameter can
     * be passed to output the stats publicly (if true) or only to the admin (if false)
     */
    slash("admin-total-usage", "get total OpenAI API usage stats") {
        execute(publicArg) {
            val public = args.first
            val msg = "```\n${database.getAPIUsage()}\n```"
            if (public) {
                respondPublic(msg)
            } else {
                respond(msg)
            }
        }
    }

    /**
     * get stats about the most recent chat interaction for the given user, or most recent overall if no user is
     * specified
     */
    slash("admin-get-last-chat", "get the last chat exchange for a user") {
        execute(UserArg("user", "user to get the most recent chat for").optional { it.author }, publicArg) {
            val (user, public) = args
            val lastExchange = database.getLastChatExchange(user.id.value)
            val response = "last exchange for ${user.tag}:\n```\n${lastExchange.toString()}\n```".trimIndent()
            if (public) {
                respondPublic(response)
            } else {
                respond(response)
            }
        }
    }

    /**
     * get stats about the most recent image generation interaction for the given user, or most recent overall if no
     * user is specified
     */
    slash("admin-get-last-image", "get the last image exchange for a user") {
        execute(UserArg("user", "user to get the most recent image for").optional { it.author }, publicArg) {
            val (user, public) = args
            val lastImage = database.getLastImage(user.id.value)
            val response = "last image for ${user.tag}:\n```\n${lastImage.toString()}\n```".trimIndent()
            if (public) {
                respondPublic(response)
            } else {
                respond(response)
            }
        }
    }

    /**
     * add a channel to the allow list
     */
    slash("admin-add-channel", "add a channel to the allow list") {
        execute(AnyArg("ChannelID", "snowflake ID of the channel to allow")) {
            val channelId = args.first.toULong()
            config.edit {
                allowChannels.add(channelId)
            }
            val channel = discord.kord.getChannel(Snowflake(channelId))
            respond("added channel ${channel?.data?.name} to allow list")
        }
    }

    /**
     * add a user to the unlimited use list
     */
    slash("admin-add-unlimited-user", "add a user to the unlimited users list") {
        execute(AnyArg("UserID", "snowflake ID of the user to add to the unlimited list")) {
            val userId = args.first.toULong()
            config.edit {
                unlimitedUsers.add(userId)
            }
            val user = discord.kord.getUser(Snowflake(userId))
            respond("added user ${user?.tag} to unlimited user list")
        }
    }

    /**
     * display the list of allowed channels
     */
    slash("admin-show-channels", "list channels this bot is allowed to respond to commands in") {
        execute {
            val channels: MutableList<String> = ArrayList()
            for (channelId in config.allowChannels) {
                discord.kord.getChannel(Snowflake(channelId))?.let {
                    val guild = discord.kord.getGuildOrNull(it.data.guildId.value!!)
                    channels.add("${guild?.name}:${it.data.name.value} (${channelId})")
                }
            }
            respond("allowed channels: [${channels.joinToString(", ")}]")
        }
    }

    /**
     * display the list of unlimited-use users
     */
    slash("admin-show-users", "list users in the unlimited use list") {
        execute {
            val userNames: MutableMap<ULong, String> = HashMap()
            for (userId in config.unlimitedUsers) {
                discord.kord.getUser(Snowflake(userId))?.let { userNames.put(userId, it.tag) }
            }
            respond(
                "unlimited use users: [${
                    userNames.entries.stream().map { "${it.value}(${it.key})" }.collect(Collectors.toList())
                        .joinToString(", ")
                }]"
            )
        }
    }

    /**
     * a right-click context menu 'OpenAPI Usage' that will output the API usage stats for the specified user privately
     * to the administrator
     */
    user(
        displayText = "OpenAPI Usage",
        slashName = "context-admin-user-usage",
        description = "get OpenAPI usage stats for this user"
    ) {
        val user = arg
        respond(
            StringFormatUtils.getUsageString(
                user, config, database.getAPIUsage(user.id.value), database.getAPIUsage(user.id.value, true)
            )
        )
    }
}