package com.st0nefish.discord.openai.preconditions

import com.st0nefish.discord.openai.data.Config
import com.st0nefish.discord.openai.data.Constants
import me.jakejmattson.discordkt.dsl.precondition

/**
 * Bot precondition to prevent other bots from using the commands for this bot
 */
@Suppress("unused")
fun botPrecondition() = precondition {
    if (author.isBot) {
        fail("bots are not permitted to use discord-gpt commands")
    }
}

/**
 * Guild precondition to check if the allowed guilds configuration option is present and if yes checks if the command
 * came from one of the allowed guilds
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun guildPrecondition(config: Config) = precondition {
    if (guild != null && config.allowGuilds.isNotEmpty() && !config.allowGuilds.contains(guild?.id?.value)) {
        fail("server ${guild?.name} is not in the allow list")
    }
}

/**
 * Channel precondition to check if the allowed channels configuration option is present and yes checks if the command
 * came from one of the allowed channels
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun channelPrecondition(config: Config) = precondition {
    if (config.allowChannels.isNotEmpty() && !config.allowChannels.contains(channel.id.value)) {
        fail("channel ${channel.data.name} is not in the allow list")
    }
}

/**
 * Admin precondition to check if the user executing admin-only commands is the bot owner
 *
 * @param config bot Config object
 */
@Suppress("unused")
fun adminPrecondition(config: Config) = precondition {
    if (command?.category == Constants.ADMIN_CATEGORY && author.id.value != config.owner.value) {
        val owner = discord.kord.getUser(config.owner)
        fail("only my overlord ${owner?.tag} may access admin commands")
    }
}