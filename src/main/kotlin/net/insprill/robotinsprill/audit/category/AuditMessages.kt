package net.insprill.robotinsprill.audit.category

import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.audit.AuditColor
import net.insprill.robotinsprill.audit.AuditManager

class AuditMessages(robot: RobotInsprill, audit: AuditManager) : AuditCategory(robot, audit) {

    override fun registerEvents() {
        val config = robot.config.audit.events.messages
        fun isIgnored(id: Snowflake): Boolean {
            return robot.config.audit.ignoreChannels.contains(id)
        }
        event<MessageDeleteEvent>(config.deleted) {
            if (guildId != robot.config.guildId) return@event
            if (isIgnored(channelId)) return@event
            send(
                message?.author,
                AuditColor.RED,
                "Message sent by <@${message?.author?.id}> deleted in <#${channelId}>.",
                message?.content
            )
        }
        event<MessageUpdateEvent>(config.edited) {
            if (new.guildId.value != robot.config.guildId) return@event
            if (isIgnored(channelId)) return@event
            if (new.content.value == null) return@event
            send(
                getMessage().author,
                AuditColor.ORANGE,
                "Message sent by <@${getMessage().author?.id}> edited in <#${channelId}>.",
                """
                **Old Message**
                ${old?.content}

                **New Message**
                ${new.content.value}
                """.trim()
            )
        }
        event<MessageCreateEvent>(config.invitePosted) {
            if (guildId != robot.config.guildId) return@event
            if (isIgnored(message.channelId)) return@event
            if (!INVITE_PATTERN.containsMatchIn(message.content)) return@event
            send(
                message.author,
                AuditColor.ORANGE,
                "Invite posted by <@${message.author?.id}> in <#${message.channelId}>.",
                message.content
            )
        }
    }

    companion object {
        // https://github.com/DV8FromTheWorld/JDA/blob/d57a7765db9d5f0fda80a04ac54c84943a07d8f6/src/main/java/net/dv8tion/jda/api/entities/Message.java#L211-L217
        val INVITE_PATTERN: Regex = Regex(
            "(?:https?://)?" +                  // Scheme
                "(?:\\w+\\.)?" +                       // Subdomain
                "discord(?:(?:app)?\\.com" +           // Discord domain
                "/invite|\\.gg)/(?<code>[a-z0-9-]+)" + // Path
                "(?:\\?\\S*)?(?:#\\S*)?",              // Useless query or URN appendix
            RegexOption.IGNORE_CASE
        )
    }

}
