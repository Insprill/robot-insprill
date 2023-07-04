package net.insprill.robotinsprill.audit

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.datetime.Clock
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.audit.category.AuditMembers
import net.insprill.robotinsprill.audit.category.AuditMessages
import net.insprill.robotinsprill.audit.category.AuditServer

class AuditManager(private val robot: RobotInsprill) {

    private val channelCache: MutableMap<Snowflake, MessageChannel> = HashMap()

    fun setupEventHandlers() {
        arrayOf(
            AuditMembers(robot, this),
            AuditMessages(robot, this),
            AuditServer(robot, this),
        ).forEach { it.registerEvents() }
    }

    suspend fun sendUserMessage(
        user: User,
        color: AuditColor,
        title: String,
        description: String?
    ) {
        if (!robot.config.audit.logBots && user.isBot) return
        val embed = buildUserEmbed(user, color, title, description)
        sendMessage(embed)
    }

    suspend fun sendServerMessage(
        color: AuditColor,
        title: String,
        footer: String?,
    ) {
        val embed = buildServerEmbed(color, title, footer)
        sendMessage(embed)
    }

    private suspend fun sendMessage(embed: EmbedBuilder.() -> Unit) {
        channelCache.getOrPut(robot.config.guildId) {
            robot.kord.getGuildOrThrow(robot.config.guildId)
                .getChannel(robot.config.audit.auditChannel) as MessageChannel
        }.createEmbed(embed)
    }

    private fun buildUserEmbed(
        user: User,
        color: AuditColor,
        title: String,
        description: String?
    ): EmbedBuilder.() -> Unit {
        return {
            author {
                name = user.tag
                icon = user.avatar?.url
            }
            this.color = color.color
            this.description = "**$title**"
            description?.let { this.description += "\n\n$description" }
            this.footer = EmbedBuilder.Footer().apply { text = "User ID: ${user.id.value}" }
            this.timestamp = Clock.System.now()
        }
    }

    private fun buildServerEmbed(
        color: AuditColor,
        title: String,
        footer: String?,
    ): EmbedBuilder.() -> Unit {
        return {
            this.color = color.color
            this.description = "**$title**"
            footer?.let {
                this.footer = EmbedBuilder.Footer().apply { text = it }
                this.timestamp = Clock.System.now()
            }
        }
    }

}
