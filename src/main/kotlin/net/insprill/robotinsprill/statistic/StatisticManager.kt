package net.insprill.robotinsprill.statistic

import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.channel.VoiceChannel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.insprill.robotinsprill.RobotInsprill
import kotlin.concurrent.timer

class StatisticManager(private val robot: RobotInsprill) {

    suspend fun start(period: Long) = coroutineScope {
        timer(daemon = true, period = period) {
            val scope = robot.kord + SupervisorJob(robot.kord.coroutineContext.job)
            scope.launch {
                update()
            }
        }
    }

    private suspend fun update() {
        robot.logger.info("Updating statistic channels")
        robot.config.statisticChannels.forEach {
            val guild = robot.kord.getGuildOrNull(robot.config.guildId)
            if (guild == null) {
                robot.logger.error("Failed to find guild ${it.channelId}")
                return@forEach
            }

            val channel = guild.getChannelOrNull(it.channelId)
            if (channel == null) {
                robot.logger.error("Failed to find channel ${it.channelId}")
                return@forEach
            }
            if (channel !is VoiceChannel) {
                robot.logger.error("Channel ${it.channelId} (#${channel.name}) isn't a voice channel!")
                return@forEach
            }

            channel.edit {
                name = it.format.format(it.statistic.func(robot, guild, it.data))
            }
        }
    }

}
