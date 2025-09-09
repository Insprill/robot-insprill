package net.insprill.robotinsprill.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildMessageCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.collect
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.command.message.MessageCommand
import net.insprill.robotinsprill.command.slash.SlashCommand

class CommandManager(private val robot: RobotInsprill) {

    private val slashCommands = HashMap<String, SlashCommand>()
    private val messageCommands = HashMap<String, MessageCommand>()

    fun setupEventHandlers() {
        robot.kord.on<GuildChatInputCommandInteractionCreateEvent> {
            slashCommands[interaction.invokedCommandName]?.execute(this)
        }
        robot.kord.on<GuildMessageCommandInteractionCreateEvent> {
            messageCommands[interaction.invokedCommandName]?.execute(this)
        }
    }

    suspend fun registerCommands(sCommands: Iterable<SlashCommand>, mCommands: Iterable<MessageCommand>) {
        robot.kord.createGuildApplicationCommands(robot.config.guildId) {
            sCommands.filter { it.enabled }.forEach { command ->
                input(command.name, command.description) { command.setup(this) }
                slashCommands[command.name] = command
            }
            robot.logger.info { "Registered ${slashCommands.size} slash commands" }

            mCommands.filter { it.enabled }.forEach { command ->
                message(command.name) { command.setup(this) }
                messageCommands[command.name] = command
            }
            robot.logger.info { "Registered ${messageCommands.size} message commands" }
        }.collect()
    }

}
