package net.insprill.robotinsprill.extension

import dev.kord.rest.builder.message.create.MessageCreateBuilder
import net.insprill.robotinsprill.configuration.BotConfig

fun MessageCreateBuilder.message(msg: BotConfig.Message) = apply {
    this.content = msg.text
    msg.embeds()?.let {
        if (this.embeds == null) this.embeds = mutableListOf()
        this.embeds?.addAll(it)
    }
}
