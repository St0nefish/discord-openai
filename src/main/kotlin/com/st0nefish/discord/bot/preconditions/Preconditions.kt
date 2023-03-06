package com.st0nefish.discord.bot.preconditions

import com.st0nefish.discord.bot.data.Config
import com.st0nefish.discord.bot.data.Constants
import me.jakejmattson.discordkt.dsl.precondition

// block all commands from bots
@Suppress("unused")
fun botPrecondition() = precondition {
    if (author.isBot) {
        fail("bots are not permitted to use discord-gpt commands")
    }
}

// only process messages from allowed guilds if configured
@Suppress("unused")
fun guildPrecondition(config: Config) = precondition {
    if (guild != null && config.allowGuilds.isNotEmpty() && !config.allowGuilds.contains(guild?.id?.value)) {
        fail("server ${guild?.name} is not in the allow list")
    }
}

// only process messages from allowed channels if configured
@Suppress("unused")
fun channelPrecondition(config: Config) = precondition {
    if (config.allowChannels.isNotEmpty() && !config.allowChannels.contains(channel.id.value)) {
        fail("channel ${channel.data.name} is not in the allow list")
    }
}

// admin commands can only be executed by the bot owner
@Suppress("unused")
fun adminPrecondition(config: Config) = precondition {
    if (command?.category == Constants.ADMIN_CATEGORY && author.id.value != config.owner.value) {
        val owner = discord.kord.getUser(config.owner)
        fail("only my overlord ${owner?.tag} may access admin commands")
    }
}