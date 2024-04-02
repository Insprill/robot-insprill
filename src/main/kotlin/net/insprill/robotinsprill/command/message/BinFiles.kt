package net.insprill.robotinsprill.command.message

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import io.ktor.util.logging.error
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.extension.stringContent

class BinFiles(private val robot: RobotInsprill) : MessageCommand() {

    override val name: String
        get() = "Bin Files"
    override val enabled: Boolean
        get() = robot.config.commands.message.binfiles.enabled

    override suspend fun execute(context: MessageCommandInteractionCreateEvent) {
        val response = context.interaction.deferPublicResponse()

        if (context.interaction.getTarget().attachments.isEmpty()) {
            response.respond {
                embed {
                    title = "That message doesn't have any attachments!"
                }
            }
            return
        }

        val bins: MutableMap<String, String> = HashMap()
        context.interaction.getTarget().attachments.forEach { attachment ->
            bins[attachment.filename] = attachment.stringContent().fold({ content ->
                robot.config.codebin.upload.uploadBin(robot.config, content)
                    .fold({ url -> url }, { err ->
                        robot.logger.error(err)
                        err.message ?: "An error occurred 1"
                    })
            }, { err ->
                robot.logger.error(err)
                err.message ?: "An error occurred 2"
            })
        }

        response.respond {
            embed {
                title = "Binned Files"
                bins.forEach { (filename, url) ->
                    field {
                        name = filename
                        value = url
                    }
                }
            }
        }
    }

}
