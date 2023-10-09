package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.Config
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.interaction.ApplicationCommandInteraction

class AccessManager {
    companion object {
        /**
         * check if the input user is an admin
         *
         * @param user {@link ULong} ID of the user to check
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is this user an admin
         */
        fun isAdminUser(user: ULong, config: Config = Config.instance()): Boolean = config.adminUsers.contains(user)

        /**
         * check if the input user is on the unlimited use list
         *
         * @param user {@link ULong} ID of the user to check
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is this user on the unlimited use list
         */
        fun isUnlimitedUser(user: ULong, config: Config = Config.instance()): Boolean =
            config.unlimitedUsers.contains(user)

        /**
         * is the current channel on the allow list
         *
         * @param channel {@link ULong} ID of the channel to check
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is this channel on the allow list
         */
        fun isAllowedChannel(channel: ULong, config: Config = Config.instance()): Boolean =
            config.allowChannels.contains(channel)

        /**
         * is the current user on the list to allow usage via private message
         *
         * @param user {@link ULong} ID of the user to check
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is the current user on the allow list
         */
        fun isAllowedUser(user: ULong, config: Config = Config.instance()): Boolean = config.allowUsers.contains(user)

        /**
         * was the current command executed by a bot admin
         *
         * @param interaction {@link ApplicationCommandInteraction} current command context
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is admin access allowed
         */
        fun allowAdminAccess(interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean =
            isAdminUser(interaction.user.id.value, config)

        /**
         * check if the current interaction satisfies standard command requirements
         *
         * @param interaction {@link ApplicationCommandInteraction} current command context
         * @param config {@link Config} configuration object for this bot
         * @return {@link Boolean} is standard access allowed?
         */
        suspend fun allowStandardAccess(
            interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean {
            return if (interaction.user.isBot) {
                // never allow bots
                false
            } else if (allowAdminAccess(interaction, config)) {
                // always allow admins
                true
            } else {
                // otherwise access depends on where the message came from
                return when (interaction.getChannel().type) {
                    // guild messages allowed only in select channels (if enabled)
                    ChannelType.GuildText -> {
                        config.allowChannels.isEmpty() || isAllowedChannel(interaction.getChannel().id.value)
                    }
                    // direct messages allowed only from select users (if enabled)
                    ChannelType.DM -> {
                        config.allowUsers.isEmpty() || isAllowedUser(interaction.user.id.value)
                    }
                    // otherwise assume not allowed
                    else -> false
                }
            }
        }
    }
}