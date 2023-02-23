package net.insprill.robotinsprill.audit.category

import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.channel.ChannelDeleteEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.audit.AuditColor
import net.insprill.robotinsprill.audit.AuditManager
import net.insprill.robotinsprill.extension.prettyName

class AuditServer(robot: RobotInsprill, audit: AuditManager) : AuditCategory(robot, audit) {

    override fun registerEvents() {
        val roleConfig = robot.config.audit.events.server.role
        val channelConfig = robot.config.audit.events.server.channel
        event<RoleCreateEvent>(roleConfig.created) {
            if (guildId != robot.config.guildId) return@event
            send(AuditColor.GREEN, "Role <@&${role.id}> was created.", "Role ID: ${role.id}")
        }
        event<RoleDeleteEvent>(roleConfig.deleted) {
            if (guildId != robot.config.guildId) return@event
            send(AuditColor.RED, "Role @${role?.name} was deleted.", "Role ID: $roleId")
        }
        event<ChannelCreateEvent>(channelConfig.created) {
            if (channel is GuildChannel && (channel as GuildChannel).guildId != robot.config.guildId) return@event
            send(
                AuditColor.GREEN,
                "${channel.type.prettyName()} channel <#${channel.id}> was created.",
                "Channel ID: ${channel.id}"
            )
        }
        event<ChannelDeleteEvent>(channelConfig.deleted) {
            if (channel is GuildChannel && (channel as GuildChannel).guildId != robot.config.guildId) return@event
            send(
                AuditColor.RED,
                "${channel.type.prettyName()} channel #${channel.data.name.value} was deleted.",
                "Channel ID: ${channel.id}"
            )
        }
    }

}
