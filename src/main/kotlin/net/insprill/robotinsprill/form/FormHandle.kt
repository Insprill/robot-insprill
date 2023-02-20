package net.insprill.robotinsprill.form

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.InteractionType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.updateEphemeralMessage
import dev.kord.core.entity.Embed
import dev.kord.core.entity.User
import dev.kord.core.entity.component.ButtonComponent
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.configuration.BotConfig
import java.net.MalformedURLException
import java.net.URL
import java.util.Collections

class FormHandle(val robot: RobotInsprill) {

    fun setupEventHandlers() {
        robot.kord.on<ModalSubmitInteractionCreateEvent> {
            handleForm(interaction)
        }
        robot.kord.on<ComponentInteractionCreateEvent> {
            handleComponent(interaction)
        }
        robot.kord.on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            if (hasManageMessage(message.author ?: return@on)) return@on
            if (robot.config.forms.any { it.channel == message.channelId } )
                message.delete("Form only channel")
        }
    }

    private suspend fun handleForm(interaction: ModalSubmitInteraction) {
        if (interaction.type != InteractionType.ModalSubmit) return

        val form = robot.config.forms.firstOrNull { it.name == interaction.modalId } ?: return // Kotlin :o
        val invalids = form.fields.filter { it.isNumber == true }.filter {
            try {
                return@filter !it.range().contains( // just in case smashed in a few more currencies
                    Integer.parseInt(interaction.textInputs[it.name]?.value?.replace(Regex("[.,$€£]"), ""))
                )
            } catch (e: NumberFormatException) {
                return@filter true
            }
        }
        val actionRow = makeInteractions(form)
        val embed = makeEmbed(interaction, form, invalids)

        val message: MessageCreateBuilder.() -> Unit = {
            embed(embed)

            if (invalids.isEmpty()) components.add(actionRow)
            else content = "**Uh oh**"
        }

        if (invalids.isEmpty()) {
            interaction.channel.createMessage(builder = message)
        } else {
            val channel = interaction.user.getDmChannelOrNull()
            if (channel == null) interaction.updateEphemeralMessage(message)
            else channel.createMessage(builder = message)
        }
    }

    private suspend fun handleComponent(interaction: ComponentInteraction) {
        if (interaction.component !is ButtonComponent) return // wtf !is

        val component = interaction.component as ButtonComponent
        val embed = interaction.message.embeds.firstOrNull() ?: return

        // Assume that embed message with button interaction means it's a form submission

        if (embed.footer?.text?.contains(interaction.user.id.toString()) != true) {
            if (!hasManageMessage(interaction.user)) return
        }

        if (component.customId == "abandon")
            try { interaction.message.delete("Abandoned by ${interaction.user.tag}") }
            catch (ignored: KtorRequestException) {}
        else {
            interaction.message.edit {
                content = "**Completed sail**"
                embeds = Collections.singletonList(strikeEmbed(embed))
                components = Collections.singletonList(makeInteractions(null))
            }
            interaction.respondEphemeral { content = "Congrats!" }
        }
    }

    private suspend fun hasManageMessage(user: User): Boolean {
        val member = user.fetchMemberOrNull(robot.config.guildId) ?: return false
        return member.getPermissions().contains(Permission.ManageMessages)
    }

    private suspend fun makeEmbed(
        interaction: ModalSubmitInteraction,
        form: BotConfig.Form,
        invalids: List<BotConfig.Form.Field>
    ): EmbedBuilder.() -> Unit {
        val self =  robot.kord.getSelf()
        val avatar = self.avatar ?: self.defaultAvatar
        return {
            val titleId = form.fields.firstOrNull { it.isTitle == true }?.name

            color = form.color

            val imageField = form.fields.firstOrNull { it.isImage == true }

            if (imageField != null) {
                val raw = interaction.textInputs[imageField.name]?.value
                try {
                    image = URL(raw).toString()
                } catch (ignored: MalformedURLException) {
                }
            }

            if (invalids.isEmpty()) {
                title = interaction.textInputs[titleId]?.value
                    ?: interaction.textInputs.values.firstOrNull()?.value
                        ?: "Submission by ${interaction.user.tag}" // Kotlin :O

                author = EmbedBuilder.Author().apply {
                    name = interaction.user.tag
                    icon = interaction.user.avatar?.url
                }
            } else title = "Your form (pls fix)"

            timestamp = Clock.System.now()

            footer = EmbedBuilder.Footer().apply {
                text = "${interaction.user.id}"
                icon = avatar.url
            }

            for (field in form.fields.filter { it.isTitle != true && it.isImage != true }) {
                field(field.name + (if (invalids.contains(field)) " `FIXME`" else ""), field.inline ?: false) {
                    interaction.textInputs[field.name]?.value ?: "_None provided._"
                }
            }
        }
    }

    private fun strikeEmbed(embed: Embed): EmbedBuilder {
        fun strike(str: String?) = if (str == null) "" else "~~$str~~"
        return EmbedBuilder().apply {
            author = EmbedBuilder.Author().apply {
                name = embed.author?.name
                icon = embed.author?.iconUrl
            }
            title = strike(embed.title)
            description = strike(embed.description)
            if (embed.image != null) image = embed.image!!.url
            footer {
                if (embed.footer?.text != null) text = embed.footer?.text!!
                icon = embed.footer?.iconUrl
            }
            for (field in embed.fields) {
                field(strike(field.name), field.inline == true) {
                    strike(field.value)
                }
            }
        }
    }

    private fun makeInteractions(form: BotConfig.Form?): ActionRowBuilder {
        val actionRow = ActionRowBuilder()

        actionRow.interactionButton(style = ButtonStyle.Danger, customId = "abandon", builder = {
            label = "Abandon ship"
            emoji = DiscordPartialEmoji(name = "✖️")
        })

        if (form?.completable == true) actionRow.interactionButton(
            style = ButtonStyle.Success,
            customId = "complete",
            builder = {
                label = "Complete sail"
                emoji = DiscordPartialEmoji(name = "✔️")
            })

        return actionRow
    }

}
