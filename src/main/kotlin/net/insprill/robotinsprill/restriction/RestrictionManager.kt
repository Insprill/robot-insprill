package net.insprill.robotinsprill.restriction

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.configuration.BotConfig
import net.insprill.robotinsprill.extension.message
import java.util.EnumSet

class RestrictionManager(val robot: RobotInsprill) {

    fun registerEvents() {
        robot.kord.on<MessageCreateEvent> {
            handle(message)
        }
    }

    private suspend fun handle(message: Message) {
        if (message.getGuildOrNull()?.id != robot.config.guildId) return
        if (message.author?.isBot == true) return

        for (channel in robot.config.restrictedChannels) {
            if (message.channelId != channel.channelId)
                return

            val containedTypes = EnumSet.allOf(MessageType::class.java).filter { it.doesContain.invoke(message) }
            if (channel.types.none { containedTypes.contains(it) }) {
                deleteWithResponse(message, channel.message)
            }
        }
    }

    private suspend fun deleteWithResponse(message: Message, response: BotConfig.Message) {
        val reply = message.reply { message(response) }
        delay(5000)
        message.delete()
        reply.delete()
    }

}
