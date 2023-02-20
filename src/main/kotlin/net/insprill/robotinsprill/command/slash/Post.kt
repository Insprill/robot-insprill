package net.insprill.robotinsprill.command.slash

import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.channel
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.form.FieldSize

class Post(private val robot: RobotInsprill) : SlashCommand() {
    override val description: String
        get() = "Post in the current channel"

    override suspend fun execute(context: ChatInputCommandInteractionCreateEvent) {
        val form =
            robot.config.forms.firstOrNull() { it.channel == (context.interaction.command.channels["target"] ?: context.interaction.channel).id }

        if (form == null) {
            context.interaction.deferEphemeralResponse()
                .respond { content = "There is not forms to fill out here u dummy" }
            return
        }

        val builders = ArrayList<ActionRowBuilder>()

        for (field in form.fields) {
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

    override val name: String
        get() = "post"
    override val enabled: Boolean
        get() = robot.config.commands.slash.post.enabled

    override fun setup(builder: ChatInputCreateBuilder) {
        builder.apply {
            channel("target", "Where would you like to post?")
        }
    }
}
