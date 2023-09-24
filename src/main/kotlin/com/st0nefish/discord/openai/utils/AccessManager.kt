package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.Config
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.interaction.ApplicationCommandInteraction

/**
 * was the current command executed by the bot owner
 *
 * @param interaction current command interaction
 * @param config current config object
 * @return was the command executed by the bot owner
 */
fun cmdByOwner(interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean =
    interaction.user.id == config.owner

/**
 * was the current command executed by a bot admin
 *
 * @param interaction current command interaction
 * @param config current config object
 * @return was the command executed by a bot admin
 */
fun cmdByAdmin(interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean =
    cmdByOwner(interaction, config) || config.admins.contains(interaction.user.id.value)

/**
 * check if the current interaction satisfies standard command requirements
 *
 * @param interaction the Command Interaction to check
 * @param config current configuration state
 * @return Boolean - is access allowed?
 */
suspend fun allowStandardAccess(
    interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean {
    // never allow bots
    if (interaction.user.isBot) return false

    // always allow admins
    if (cmdByAdmin(interaction, config)) return true

    // otherwise access depends on where the message came from
    return when (interaction.getChannel().type) {
        // guild messages allowed only in select channels
        ChannelType.GuildText -> {
            config.allowChannels.contains(interaction.getChannel().id.value)
        }
        // direct messages allowed only from select users
        ChannelType.DM -> {
            config.allowUsers.contains(interaction.user.id.value)
        }
        // otherwise assume not allowed
        else -> false
    }
}

/**
 * check if the current interaction satisfies admin command requirements
 *
 * @param interaction the command interaction to check
 * @param config current configuration state
 * @return Boolean - is access allowed
 */
fun allowAdminAccess(interaction: ApplicationCommandInteraction, config: Config = Config.instance()): Boolean {
    return cmdByAdmin(interaction, config)
}