package net.insprill.robotinsprill.form

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.InteractionType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.Embed
import dev.kord.core.entity.User
import dev.kord.core.entity.component.ButtonComponent
import dev.kord.core.entity.component.SelectMenuComponent
import dev.kord.core.entity.component.TextInputComponent
import dev.kord.core.entity.component.UserSelectComponent
import dev.kord.core.entity.interaction.ComponentInteraction
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.configuration.BotConfig
import java.net.MalformedURLException
import java.net.URL
import java.util.Arrays
import java.util.Collections
import java.util.function.Function
import kotlin.math.roundToInt

class FormHandle(val robot: RobotInsprill) {

    companion object {
        @JvmStatic
        private val REGEX = Regex("[,$€£]")
    }

    fun setupEventHandlers() {
        robot.kord.on<ModalSubmitInteractionCreateEvent> {
            handleForm(interaction)
        }
        robot.kord.on<ComponentInteractionCreateEvent> {
            if (interaction.component is ButtonComponent) handleButtonComponent(interaction)
            else handleInputComponent(interaction)
        }
        robot.kord.on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            if (hasManageMessage(message.author ?: return@on)) return@on
            if (robot.config.forms.list.any { it.channel == message.channelId && it.formsOnly == true })
                message.delete("Form only channel")
        }
    }

    private suspend fun handleForm(interaction: ModalSubmitInteraction) {
        if (interaction.type != InteractionType.ModalSubmit) return

        val form = robot.config.forms.list.firstOrNull { it.name == interaction.modalId } ?: return // Kotlin :o
        val invalids = form.getInputFields().filter { it.isNumber == true }.filterNot {
            return@filterNot it.range().contains(
                toNumber(
                    interaction.textInputs[it.name]?.value?.replace(REGEX, "")
                ) ?: return@filterNot false
            )
        }
        val buttons = makeButtons(form, false)
        val inputs = makeInputs(form)
        val embed =
            makeEmbed(interaction.user, interaction.textInputs, form, MutableList(invalids.size) { invalids[it] })

        if (invalids.isEmpty()) { // Everything is good to go
            interaction.respondPublic {
                embed(embed)
                components.add(buttons)
                if (inputs != null) components.add(inputs)
            }
        } else { // Needs fixing (number is malformed, etc.)
            interaction
                .respondEphemeral(robot.config.forms.findMessage("malformed-input", "Pls fix!")!!.toBuilder())
                .createEphemeralFollowup {
                    embed(embed)
                }
        }

    }

    private suspend fun handleButtonComponent(interaction: ComponentInteraction) {
        if (interaction.component !is ButtonComponent) return // wtf !is

        val form = robot.config.forms.list.firstOrNull { it.channel == interaction.channelId }
        val component = interaction.component as ButtonComponent
        val embed = interaction.message.embeds.firstOrNull() ?: return

        // Assume that embed message with button interaction means it's a form submission

        if (embed.footer?.text?.contains(interaction.user.id.toString()) != true) {
            if (!hasManageMessage(interaction.user)) return
        }

        when (component.customId) {
            "abandon" -> try {
                interaction.message.delete("Abandoned by ${interaction.user.username}")
            } catch (ignored: KtorRequestException) {
            }

            "complete" -> {
                interaction.message.edit {
                    content = "**Completed sail**"
                    embeds = Collections.singletonList(editEmbed(embed) { if (it == null) "" else "~~$it~~" })
                    components = Collections.singletonList(makeButtons(null, false))
                }
            }

            "reset" -> {
                val embedBuilder = editEmbed(embed) { it ?: "" }.apply {
                    for (field in form!!.getPostSubmissionFields()) {
                        fields.removeIf { it.name == field.name }
                        field(field.name, field.inline == true) { "_None_." }
                    }
                }
                interaction.message.edit {
                    embeds = Collections.singletonList(embedBuilder)
                }
            }
        }

        interaction.respondEphemeral(robot.config.forms.findMessage("button-pressed", "Got it boss")!!.toBuilder())
    }

    private suspend fun handleInputComponent(interaction: ComponentInteraction) {
        if (interaction.component !is UserSelectComponent) return

        val form = robot.config.forms.list.firstOrNull { it.channel == interaction.channelId }
        val input = interaction.component as SelectMenuComponent
        val embed = interaction.message.embeds.firstOrNull() ?: return
        val memberData = interaction.data.data.resolvedObjectsData.value?.members?.value?.values?.firstOrNull()

        if (embed.footer?.text?.contains(interaction.user.id.toString()) != true) {
            if (!hasManageMessage(interaction.user)) return
        }

        val embedBuilder = editEmbed(embed) { it ?: "" }.apply {
            val old = fields.firstOrNull { it.name == input.data.customId.value } ?: return
            val index = fields.indexOf(old)

            val newField = EmbedBuilder.Field().apply {
                name = input.data.customId.value ?: return
                inline = old.inline == true
                value = "<@${memberData?.userId}>"
            }

            fields.remove(old)
            fields.add(index, newField)
        }

        interaction.message.edit {
            embeds = Collections.singletonList(embedBuilder)
            components = Arrays.asList(makeButtons(form, true), makeInputs(form))
        }

        interaction.respondEphemeral(robot.config.forms.findMessage("button-pressed", "Got it boss")!!.toBuilder())
    }

    private suspend fun hasManageMessage(user: User): Boolean {
        val member = user.fetchMemberOrNull(robot.config.guildId) ?: return false
        return member.getPermissions().contains(Permission.ManageMessages)
    }

    private suspend fun makeEmbed(
        user: User,
        textInputs: Map<String, TextInputComponent>,
        form: BotConfig.Forms.Form,
        invalids: MutableList<BotConfig.Forms.Form.Field>
    ): EmbedBuilder.() -> Unit {
        val self = robot.kord.getSelf()
        val avatar = self.avatar ?: self.defaultAvatar
        return {
            val titleId = form.fields.firstOrNull { it.isEmbedTitle == true }?.name

            color = form.color

            val imageField = form.fields.firstOrNull { it.isImage == true }

            if (imageField != null) {
                val raw = textInputs[imageField.name]?.value ?: ""
                if (raw.isNotBlank()) try {
                    image = URL(raw).toString()
                } catch (ignored: MalformedURLException) {
                }
            }

            if (invalids.isEmpty()) {
                title = textInputs[titleId]?.data?.value?.value
                    ?: textInputs.values.firstOrNull()?.data?.value?.value
                        ?: "Submission by ${user.username}" // Kotlin :O

                author = EmbedBuilder.Author().apply {
                    name = user.username
                    icon = user.avatar?.cdnUrl?.toUrl()
                }
            } else title = "Your form (pls fix)"

            timestamp = Clock.System.now()

            footer = EmbedBuilder.Footer().apply {
                text = "${user.id}"
                icon = avatar.cdnUrl.toUrl()
            }

            for (field in form.getDisplayFields()) {
                field(field.name + (if (invalids.contains(field)) " `FIXME`" else ""), field.inline ?: false) {
                    textInputs[field.name]?.value ?: "_None._"
                }
            }

            if (form.addContact == true) field("Contact", false) { user.mention }
        }
    }

    private fun editEmbed(embed: Embed, stringModifier: Function<String?, String>): EmbedBuilder {
        return EmbedBuilder().apply {
            author = EmbedBuilder.Author().apply {
                name = embed.author?.name
                icon = embed.author?.iconUrl
            }
            title = stringModifier.apply(embed.title)
            description = stringModifier.apply(embed.description)
            if (embed.image != null) image = embed.image!!.url
            footer {
                if (embed.footer?.text != null) text = embed.footer?.text!!
                icon = embed.footer?.iconUrl
            }
            for (field in embed.fields) {
                field(stringModifier.apply(field.name), field.inline == true) {
                    stringModifier.apply(field.value)
                }
            }
            timestamp = embed.timestamp
        }
    }

    private fun makeButtons(form: BotConfig.Forms.Form?, canRedo: Boolean?): ActionRowBuilder {
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

        if (form?.getPostSubmissionFields()
                ?.isNotEmpty() == true
        ) actionRow.interactionButton(style = ButtonStyle.Secondary, customId = "reset") {
            label = "Redo ship"
            disabled = canRedo == false
        }

        return actionRow
    }

    private fun makeInputs(form: BotConfig.Forms.Form?): ActionRowBuilder? {
        if (form?.getPostSubmissionFields()?.isEmpty() == true) return null
        if (form == null) return null

        val actionRow = ActionRowBuilder()

        for (field in form.getPostSubmissionFields()) {
            actionRow.userSelect(field.name)
        }

        return actionRow
    }

    private fun toNumber(str: String?): Int? {
        if (str == null) return null
        return (str.toDoubleOrNull())?.roundToInt()
    }

}
