package com.st0nefish.discord.bot

import dev.kord.common.annotation.KordPreview
import me.jakejmattson.discordkt.dsl.bot


@KordPreview
fun main(args: Array<String>) {
    // discord bot token
    val token = "MTA4MTc0MjIwOTEzNDYyODkzNQ.Gvvy9I.VT3dlT9PJETrRcX62kWH4D6BqWVLbAW_Bk6PUs";

    bot(token) {
        prefix { "/" }
    }
}