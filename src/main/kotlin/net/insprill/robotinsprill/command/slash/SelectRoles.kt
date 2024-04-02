package net.insprill.robotinsprill.command.slash

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.component.StringSelectComponent
import dev.kord.core.entity.interaction.GuildSelectMenuInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildSelectMenuInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.actionRow
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.extension.message

class SelectRoles(private val robot: RobotInsprill) : SlashCommand() {

    override val name: String
        get() = "select-roles"
    override val description: String
        get() = "Allows you to select what roles you want"

    override val enabled: Boolean
        get() = robot.config.commands.slash.selectRoles.enabled

    init {
        robot.kord.on<GuildSelectMenuInteractionCreateEvent> {
            handleInteract(this.interaction)
        }
    }

    override suspend fun execute(context: ChatInputCommandInteractionCreateEvent) {
        val selectRoles = robot.config.commands.slash.selectRoles
        val guild = robot.kord.getGuildOrNull(robot.config.guildId) ?: return
        context.interaction.respondEphemeral {
            message(selectRoles.initialResponse)
            actionRow {
                stringSelect("roles") {
                    allowedValues = 1..selectRoles.roles.size
                    selectRoles.roles.forEach {
                        val role = guild.getRoleOrNull(it.id) ?: return@forEach
                        option("@${role.name}", role.id.toString()) {
                            emoji = it.emoji?.asPartialEmoji()
                            default = role.id in context.interaction.user.asMember(guild.id).roleIds
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleInteract(interaction: GuildSelectMenuInteraction) {
        val member = interaction.user.asMember()

        val existingRoles = interaction.user.asMember().roleIds
        val selectedValues = interaction.values
        val values = (interaction.component as? StringSelectComponent)?.options?.map { it.value } ?: return

        values
            .filter { it in selectedValues }
            .map { Snowflake(it) }
            .filter { it !in existingRoles }
            .forEach {
                member.addRole(it, "/$name command")
            }

        values
            .filter { it !in selectedValues }
            .map { Snowflake(it) }
            .filter { it in existingRoles }
            .forEach {
                member.removeRole(it, "/$name command")
            }

        interaction.respondEphemeral { message(robot.config.commands.slash.selectRoles.updatedResponse) }
    }

}
