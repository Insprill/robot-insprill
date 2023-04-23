package net.insprill.robotinsprill.configuration

import dev.kord.common.Color
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlinx.datetime.Instant
import net.insprill.robotinsprill.autoaction.MediaType
import net.insprill.robotinsprill.codebin.BinService
import net.insprill.robotinsprill.form.FieldSize
import net.insprill.robotinsprill.restriction.MessageType
import net.insprill.robotinsprill.statistic.Statistic

data class BotConfig(
    val guildId: Snowflake,
    val commands: Commands,
    val codebin: Bin,
    val audit: Audit,
    val statisticChannels: List<StatisticChannel>,
    val autoActions: List<AutoAction>,
    val restrictedChannels: List<RestrictedChannel>,
    val forms: Forms
) {
    data class Commands(val message: MessageCmd, val slash: Slash) {
        data class MessageCmd(val binfiles: BinFiles, val googleThat: GoogleThat) {
            data class BinFiles(val enabled: Boolean)
            data class GoogleThat(val enabled: Boolean)
        }

        data class Slash(
            val custom: List<CustomCommand>,
            val clear: Clear,
            val selectRoles: SelectRoles,
            val post: Post
        ) {
            data class CustomCommand(
                val name: String,
                val description: String,
                val enabled: Boolean = true,
                val private: Boolean = false,
                val response: Message,
            )

            data class Clear(val enabled: Boolean, val limit: Long)
            data class SelectRoles(
                val enabled: Boolean,
                val initialResponse: Message,
                val updatedResponse: Message,
                val roles: List<Roles>
            ) {
                data class Roles(val id: Snowflake, val emoji: Emoji?)
            }

            data class Post(val enabled: Boolean)
        }
    }

    data class Bin(val upload: BinService, val services: Map<BinService, List<String>>) {
        private val basePattern = "https:\\/\\/%s\\/(?<key>%s)"
        val patterns = services.mapValues { (service, domains) ->
            domains.map { it.replace(".", "\\.") }
                .map { Regex(basePattern.format(it, service.keyPattern)) }
        }
    }

    data class Audit(
        val auditChannel: Snowflake,
        val ignoreChannels: List<Snowflake>,
        val logBots: Boolean,
        val events: Events
    ) {
        data class Events(val members: Members, val messages: Messages, val server: Server) {
            data class Members(
                val banned: Boolean,
                val unbanned: Boolean,
                val joined: Boolean,
                val left: Boolean,
                val voice: Boolean,
                val updated: Updated
            ) {
                data class Updated(
                    val enabled: Boolean,
                    val nickname: Boolean,
                    val username: Boolean,
                    val avatar: Boolean,
                    val memberAvatar: Boolean,
                    val banner: Boolean,
                    val discriminator: Boolean,
                )
            }

            data class Messages(val deleted: Boolean, val edited: Boolean, val invitePosted: Boolean)
            data class Server(val role: Role, val channel: Channel) {
                data class Role(val created: Boolean, val deleted: Boolean)
                data class Channel(val created: Boolean, val deleted: Boolean)
            }
        }
    }

    data class StatisticChannel(
        val channelId: Snowflake,
        val format: String,
        val statistic: Statistic,
        val data: String?
    )

    data class Message(val text: String?, val embeds: List<Embed>?) {

        fun toBuilder(): MessageCreateBuilder.() -> Unit {
            return {
                content = text
                if (embeds.isNotEmpty()) embeds.addAll(embeds()!!)
            }
        }

        data class Embed(
            var author: EmbedAuthor?,
            val title: String?,
            var url: String?,
            val description: String?,
            var image: String?,
            var thumbnail: String?,
            var color: Color?,
            var footer: EmbedFooter?,
            var timestamp: Instant?,
//          var fields: MutableList<EmbedBuilder.Field>?,
        ) {
            data class EmbedFooter(val text: String, val icon: String?) {
                fun kord(): EmbedBuilder.Footer {
                    return EmbedBuilder.Footer().also {
                        it.text = this.text
                        it.icon = this.icon
                    }
                }
            }

            data class EmbedAuthor(val name: String?, val icon: String?, val url: String?) {
                fun kord(): EmbedBuilder.Author {
                    return EmbedBuilder.Author().also {
                        it.name = this.name
                        it.icon = this.icon
                        it.url = this.url
                    }
                }
            }
        }

        fun embeds(): List<EmbedBuilder>? {
            return this.embeds?.map {
                EmbedBuilder().apply {
                    title = it.title
                    description = it.description
                    url = it.url
                    timestamp = it.timestamp
                    color = it.color
                    image = it.image
                    footer = it.footer?.kord()
                    thumbnail = it.thumbnail?.let { EmbedBuilder.Thumbnail().apply { url = it } }
                    author = it.author?.kord()
//                  fields = it.fields ?: ArrayList()
                }
            }
        }
    }

    data class Emoji(val name: String, val id: Snowflake?, val animated: Boolean = false) {
        fun asReactionEmoji(): ReactionEmoji {
            return if (id == null) {
                ReactionEmoji.Unicode(name)
            } else {
                ReactionEmoji.Custom(id, name, animated)
            }
        }

        fun asPartialEmoji(): DiscordPartialEmoji {
            return DiscordPartialEmoji(id, name, OptionalBoolean.Value(animated))
        }
    }

    data class AutoAction(
        val channels: Set<Snowflake>?,
        val media: Set<MediaType>,
        val bots: Boolean = false,
        val actions: List<Action>
    ) {
        data class Action(val pattern: Regex, val reactions: Set<Emoji>?, val responses: List<Message>?)
    }

    data class RestrictedChannel(val channelId: Snowflake, val message: Message, val types: List<MessageType>)

    fun validate(): String? {
        if (codebin.upload == BinService.PASTEBIN && System.getenv("PASTEBIN_API_KEY") == null) {
            return "The PASTEBIN_API_KEY environment variable must be set to do uploads to pastebin!"
        }
        return null
    }

    data class Forms(val list: List<Form>, val messages: Map<String, Message>?) {

        @OptIn(ExperimentalContracts::class)
        fun findMessage(key: String, default: String?): Message? {
            contract {
                returnsNotNull() implies (default != null)
            }

            val def = if (default != null) Message(default, null) else null

            if (messages == null) return def

            val msg = messages[key]?.takeIf { it.text?.isNotBlank() == true }

            return msg ?: def
        }

        data class Form(
            val name: String,
            val channel: Snowflake,
            val color: Color,
            val completable: Boolean,
            val fields: List<Field>,
            val addContact: Boolean?,
            val formsOnly: Boolean?
        ) {

            fun getInputFields(): List<Field> {
                return fields.filterNot { it.isPostSubmission == true }
            }

            fun getPostSubmissionFields(): List<Field> {
                return fields.filter { it.isPostSubmission == true }
            }

            fun getDisplayFields(): List<Field> {
                return fields.filterNot { it.isImage == true }
            }

            data class Field(
                val size: FieldSize?,
                val isEmbedTitle: Boolean?,
                val isNumber: Boolean?,
                val name: String,
                val min: Int?,
                val max: Int?,
                val inline: Boolean?,
                val isImage: Boolean?,
                val optional: Boolean?,
                val isPostSubmission: Boolean?
            ) {
                fun range(): IntRange {
                    return (min ?: 0)..(max ?: 4000)
                }
            }
        }
    }
}


