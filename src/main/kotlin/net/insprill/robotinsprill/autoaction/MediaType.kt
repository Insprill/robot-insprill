package net.insprill.robotinsprill.autoaction

import dev.kord.core.entity.Message
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.extension.byteContent
import net.insprill.robotinsprill.extension.stringContent
import net.insprill.robotinsprill.ocr.Tesseract

@Suppress("UNUSED")
enum class MediaType {
    TEXT {
        override suspend fun findText(robot: RobotInsprill, message: Message): Iterable<String> {
            return if (message.content.isBlank()) emptyList() else listOf(message.content.trim())
        }
    },
    FILE {
        private val contentTypes = arrayOf("text", "application")

        override suspend fun findText(robot: RobotInsprill, message: Message): Iterable<String> {
            return message.attachments
                .filter { attachment ->
                    contentTypes.any { attachment.contentType?.startsWith(it) == true }
                }.mapNotNull {
                    it.stringContent().getOrNull()?.trim()
                }
        }
    },
    BIN {
        override suspend fun findText(robot: RobotInsprill, message: Message): Iterable<String> {
            return robot.config.codebin.services.flatMap { (service, domains) ->
                robot.config.codebin.patterns[service]!!
                    .flatMap { it.findAll(message.content) }
                    .mapNotNull { match ->
                        service.downloadBin(
                            domains.find { match.value.contains(it) }!!,
                            match.groups["key"]!!.value
                        ).getOrNull()?.trim()
                    }
            }
        }
    },
    IMAGE {
        private val contentTypes = arrayOf("image/png", "image/jpeg")

        override suspend fun findText(robot: RobotInsprill, message: Message): Iterable<String> {
            return if (!Tesseract.enabled) listOf() else message.attachments
                .filter { attachment ->
                    contentTypes.any { attachment.contentType?.equals(it) == true }
                }.mapNotNull { attachment ->
                    val bytes = attachment.byteContent().getOrNull() ?: return@mapNotNull null
                    Tesseract(bytes).scan().onFailure { robot.logger.error(it) { "Failed to extract text from image" } }.getOrNull()?.trim()
                }
        }
    },
    ;

    abstract suspend fun findText(robot: RobotInsprill, message: Message): Iterable<String>

}
