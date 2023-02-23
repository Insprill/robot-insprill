package net.insprill.robotinsprill.audit.category

import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import net.insprill.robotinsprill.RobotInsprill
import net.insprill.robotinsprill.audit.AuditColor
import net.insprill.robotinsprill.audit.AuditManager

class AuditMembers(robot: RobotInsprill, audit: AuditManager) : AuditCategory(robot, audit) {

    override fun registerEvents() {
        val config = robot.config.audit.events.members
        event<BanAddEvent>(config.banned) {
            if (guildId != robot.config.guildId) return@event;
            send(user, AuditColor.RED, "<@${user.id}> was banned.")
        }
        event<BanRemoveEvent>(config.unbanned) {
            if (guildId != robot.config.guildId) return@event;
            send(user, AuditColor.GREEN, "<@${user.id}> was unbanned.")
        }
        event<MemberJoinEvent>(config.joined) {
            if (guildId != robot.config.guildId) return@event;
            send(member, AuditColor.GREEN, "<@${member.id}> has joined.")
        }
        event<MemberLeaveEvent>(config.left) {
            if (guildId != robot.config.guildId) return@event;
            send(user, AuditColor.RED, "<@${user.id}> has left.")
        }
        event<MemberUpdateEvent>(config.updated.enabled) {
            if (guildId != robot.config.guildId) return@event;
            if (old == null) {
                send(member, AuditColor.GREEN, "<@${member.id}> updated their profile!")
            } else {
                var desc = ""
                val old = old!! // Already checked to be non-null
                if (config.updated.nickname && old.nickname != member.nickname) {
                    desc += "**Nickname Updated**\n${old.displayName} -> ${member.displayName}\n\n"
                }
                if (config.updated.username && old.username != member.username) {
                    desc += "**Name Updated**\n${old.username} -> ${member.username}\n\n"
                }
                if (config.updated.avatar && old.avatar?.url != member.avatar?.url) {
                    desc += "**Avatar Updated**\n${old.avatar?.url} -> ${member.avatar?.url}\n\n"
                }
                if (config.updated.memberAvatar && old.memberAvatar?.url != member.memberAvatar?.url) {
                    desc += "**Member Avatar Updated**\n${old.memberAvatar?.url} -> ${member.memberAvatar?.url}\n\n"
                }
                if (config.updated.banner && old.data.banner != member.data.banner) {
                    desc += "**Member Avatar Updated**\n${old.data.banner} -> ${member.data.banner}\n\n"
                }
                if (config.updated.discriminator && old.data.discriminator != member.discriminator) {
                    desc += "**Discriminator Updated**\n${old.data.discriminator} -> ${member.data.discriminator}\n\n"
                }
                send(member, AuditColor.GREEN, "<@${member.id}> updated their profile!", desc.trim())
            }

        }
        event<VoiceStateUpdateEvent>(config.voice) {
            if (state.guildId != robot.config.guildId) return@event;
            if (state.channelId == null) {
                send(
                    state.getMember(),
                    AuditColor.RED,
                    "<@${state.getMember().id}> has left <#${old?.channelId}>."
                )
            } else {
                send(
                    state.getMember(),
                    AuditColor.GREEN,
                    "<@${state.getMember().id}> has joined <#${state.channelId}>."
                )
            }
        }
    }

}
