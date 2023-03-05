package com.st0nefish.discord.bot.commands

import me.jakejmattson.discordkt.arguments.UserArg
import me.jakejmattson.discordkt.commands.commands

fun testCommand() = commands("test") {
    slash("test", "a test command") {
        execute {
            respond("test response")
        }
    }

    slash("mock", "insult a user") {
        execute(UserArg("user")) {
            val user = args.first
            respond("hello " + user.username)
        }
    }

    slash("hello", "say hello to user") {
        execute {
            respond("hello " + author.username)
        }
    }
}