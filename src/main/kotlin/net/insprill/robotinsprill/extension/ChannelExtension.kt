package net.insprill.robotinsprill.extension

import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.ThreadChannel

suspend fun MessageChannel.parentOrSelf(): MessageChannel {
    if (this !is ThreadChannel) return this
    return this.getParentOrNull() ?: this
}
