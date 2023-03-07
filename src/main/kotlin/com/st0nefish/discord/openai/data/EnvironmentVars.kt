package com.st0nefish.discord.openai.data

/**
 * constants class with the names of the various environment variables used by this bot
 *
 * @constructor Create empty Environment vars
 */
class EnvironmentVars private constructor() {
    companion object {
        const val BOT_OWNER: String = "DISCORD_GPT_BOT_OWNER"
        const val CMD_PREFIX: String = "DISCORD_GPT_CMD_PREFIX"
        const val BOT_TOKEN: String = "DISCORD_GPT_BOT_TOKEN"
        const val OPENAI_TOKEN: String = "DISCORD_GPT_OPENAI_TOKEN"
        const val ALLOW_GUILDS: String = "DISCORD_GPT_ALLOW_GUILDS"
        const val ALLOW_CHANNELS: String = "DISCORD_GPT_ALLOW_CHANNELS"
        const val MAX_DAILY_USER_COST: String = "DISCORD_GPT_MAX_DAILY_USER_COST"
    }
}