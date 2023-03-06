package com.st0nefish.discord.bot.data

class EnvironmentVars {
    companion object {
        const val OWNER: String = "DISCORD_GPT_BOT_OWNER"
        const val ADMIN_SERVER: String = "DISCORD_GPT_ADMIN_SERVER"
        const val CMD_PREFIX: String = "DISCORD_GPT_CMD_PREFIX"
        const val BOT_TOKEN: String = "DISCORD_GPT_BOT_TOKEN"
        const val OPENAI_TOKEN: String = "DISCORD_GPT_OPENAI_TOKEN"
        const val ALLOW_GUILDS: String = "DISCORD_GPT_ALLOW_GUILDS"
        const val ALLOW_CHANNELS: String = "DISCORD_GPT_ALLOW_CHANNELS"
    }
}