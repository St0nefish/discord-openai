package com.st0nefish.discord.openai.utils

import com.st0nefish.discord.openai.data.Config
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ApplicationCommandInteraction
import dev.kord.core.entity.interaction.ChatInputCommandInteraction

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
 * check if the current interaction is allowed
 *
 * @param interaction the Command Interaction to check
 * @param config current configuration state
 * @return Boolean - is access allowed?
 */
suspend fun allowStandardAccess(
    interaction: ChatInputCommandInteraction, config: Config = Config.instance()): Boolean {
    // never allow bots
    if (interaction.user.isBot) return false

    // always allow master
    if (interaction.user.id == config.owner) return true

    // otherwise access depends on where the message came from
    return when (interaction.channel.asChannel().type) {
        // guild messages allowed only in select channels
        ChannelType.GuildText -> {
            if (! config.allowChannels.contains(interaction.channel.asChannel().id.value)) {
                interaction.respondEphemeral { content = "I am not allowed to respond in this channel" }
                false
            } else {
                true
            }
        }
        // direct messages allowed only from select users
        ChannelType.DM -> {
            if (! config.allowUsers.contains(interaction.user.id.value)) {
                interaction.respondEphemeral { content = "I am not allowed to respond to DMs from you" }
                false
            } else {
                true
            }
        }
        // otherwise assume not allowed
        else -> false
    }
}