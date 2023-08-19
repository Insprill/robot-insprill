package net.insprill.robotinsprill.command.slash

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.integer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import net.insprill.robotinsprill.RobotInsprill
import java.lang.Long.min

class Clear(private val robot: RobotInsprill) : SlashCommand() {

    override val name: String
        get() = "clear"
    override val description: String
        get() = "Deletes the last n messages"
    override val enabled: Boolean
        get() = robot.config.commands.slash.clear.enabled

    override fun setup(builder: ChatInputCreateBuilder) {
        builder.apply {
            defaultMemberPermissions = Permissions(Permission.ManageMessages)
            integer("amount", "How many messages to clear. Messages older than 14 days may not be deleted.") {
                required = true
            }
        }
    }

    override suspend fun execute(context: ChatInputCommandInteractionCreateEvent) {
        val response = context.interaction.deferEphemeralResponse()

        val command = context.interaction.command
        val amount = min(command.integers["amount"]!! /*Can't be null*/, robot.config.commands.slash.clear.limit)

        val channel = context.interaction.getChannel()
        val lastMessage = channel.lastMessageId
        if (lastMessage == null) {
            response.respond {
                content = "Failed to find latest message! Try sending one now then rerunning the command."
            }
            return
        }

        // I don't think this is possible, but just in case.
        if (channel !is GuildMessageChannelBehavior) {
            robot.logger.error("Tried to run clear command in a non-GuildMessageChannelBehavior")
            response.respond { content = "You can't do that here!" }
            return
        }

        var idx = 0L
        val messages = (channel as GuildMessageChannelBehavior)
            .withStrategy(EntitySupplyStrategy.rest) // Cache may be out-of-date, causing the wrong messages to be deleted.
            .getMessagesBefore(lastMessage, amount.toInt())
            .takeWhile { idx++ < amount }
            .map { it.id }
            .toList()
        val deletedCount = idx - 1 // Gets incremented one extra time when idx == amount

        channel.bulkDelete(messages, true, "/clear $deletedCount run by <@${context.interaction.user.id}>")

        response.respond {
            content = "Deleted the last $deletedCount messages!"
        }
    }

}
