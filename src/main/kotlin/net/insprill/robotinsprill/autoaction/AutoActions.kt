package net.insprill.robotinsprill.autoaction

import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.extension.parentOrSelf

class AutoActions(private val robot: RobotInsprill) {

    fun setupEventHandlers() {
        robot.kord.on<MessageCreateEvent>()
        {
            handle(message)
        }
    }

    private suspend fun handle(message: Message) {
        if (message.getGuildOrNull()?.id != robot.config.guildId) return
        val channel = message.getChannel().parentOrSelf()
        val autoActions = robot.config.autoActions
            .filter { it.bots == message.author?.isBot }
            .filter { it.channels == null || channel.id in it.channels }
        val actions = autoActions.map { it.actions }.flatten()
        val strings = autoActions.flatMap { it.media }.distinct().flatMap {
            it.findText(robot, message)
        }
        actions.associateWith { strings }
            .filter { (action, string) ->
                string.any { action.pattern.containsMatchIn(it) }
            }
            .forEach { (action, _) ->
                Action.values().forEach { it.process(message, action) }
            }
    }

}
