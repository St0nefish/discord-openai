package com.st0nefish.discord.bot.listeners

import dev.kord.core.event.message.MessageCreateEvent
import me.jakejmattson.discordkt.dsl.listeners

fun testListener() = listeners {
    on<MessageCreateEvent> {
        println(message.content)
    }
}