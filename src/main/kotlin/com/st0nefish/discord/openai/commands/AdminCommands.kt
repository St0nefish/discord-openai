package com.st0nefish.discord.openai.commands

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.Constants
import com.st0nefish.discord.openai.utils.UsageTracker
import me.jakejmattson.discordkt.arguments.BooleanArg
import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.commands

/**
 * create administrator-only commands
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun adminCommands(config: Config) = commands(Constants.ADMIN_CATEGORY) {
    val usageTracker = UsageTracker.getInstance()

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
        execute(UserArg, BooleanArg.optional(false)) {
            val (user, public) = args
            if (public) {
                respondPublic(usageTracker.getUserUsageString(user))
            } else {
                respond(usageTracker.getUserUsageString(user))
            }
        }
    }

    /**
     * admin-total-usage command to get the current total API usage stats across all users. an optional parameter can
     * be passed to output the stats publicly (if true) or only to the admin (if false)
     */
    slash("admin-total-usage", "get total OpenAI API usage stats") {
        execute(BooleanArg.optional(false)) {
            val public = args.first
            if (public) {
                respondPublic(usageTracker.getTotalUsageString())
            } else {
                respond(usageTracker.getTotalUsageString())
            }
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
        respond(usageTracker.getUserUsageString(arg))
    }
}