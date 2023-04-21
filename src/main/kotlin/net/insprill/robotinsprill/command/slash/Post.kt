package net.insprill.robotinsprill.command.slash

import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.form.FieldSize

class Post(private val robot: RobotInsprill) : SlashCommand() {

    override val name: String
        get() = "post"
    override val description: String
        get() = "Post in the current channel"
    override val enabled: Boolean
        get() = robot.config.commands.slash.post.enabled

    override suspend fun execute(context: ChatInputCommandInteractionCreateEvent) {
        val form = robot.config.forms.list.firstOrNull {
            it.channel == context.interaction.channelId
        }

        if (form == null) {
            context.interaction.respondEphemeral(
                robot.config.forms.findMessage(
                    "invalid-channel",
                    "That ain't no post channel u dummy"
                )!!.toBuilder()
            )
            return
        }

        val builders = ArrayList<ActionRowBuilder>()

        for (field in form.getInputFields()) {
            val builder = ActionRowBuilder()
            builder.textInput(
                style = if (field.size == FieldSize.LONG) TextInputStyle.Paragraph else TextInputStyle.Short,
                customId = field.name,
                label = field.name,
                builder = {
                    required = field.optional != true
                    allowedLength = field.range()
                }
            )
            builders.add(builder)
        }

        context.interaction.modal(title = form.name, customId = form.name) {
            components.addAll(builders)
        }

        // Annnd wait for the submission event
    }
}
