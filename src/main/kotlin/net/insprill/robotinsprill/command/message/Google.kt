package net.insprill.robotinsprill.command.message

import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.extension.urlEncoded

class Google(private val robot: RobotInsprill) : MessageCommand() {

    override val name: String
        get() = "Google That"
    override val enabled: Boolean
        get() = robot.config.commands.message.googleThat.enabled

    override suspend fun execute(context: MessageCommandInteractionCreateEvent) {
        val content = context.interaction.getTarget().content
        context.interaction.respondPublic {
            embed {
                title = content
                url = "https://letmegooglethat.com/?q=${content.urlEncoded()}"
            }
        }
    }

}
