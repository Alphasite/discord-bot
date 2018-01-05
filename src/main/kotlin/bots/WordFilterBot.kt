package bots

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import sendEmbed
import sendMessage
import java.awt.Color
import java.nio.file.Path
import java.text.Normalizer


class WordFilterBot(
        configRoot: Path
) : BotImpl(configRoot) {
    private val deleteSymbol = "👎"
    private val keepSymbol = "👍"
    private val mute15 = "🕒"
    private val mute30 = "🕕"
    private val mute45 = "🕘"
    private val mute60 = "🕛"

    val config = Bot.loadConfig(configPath, WordFilterConfig())

    override val path get() = "filter"

    override val help: List<String>
        get() {
            return listOf()
        }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (config.filteredChannels.isNotEmpty()) {
            if (config.filteredChannels.filter { it == event.channel.name }.isEmpty()) {
                return
            }
        }

        val channel = event.guild.textChannels.find { it.name == config.flaggedForReviewChannel }

        if (channel == null) {
            println("Review Channel does not exist... Ignoring")
            return
        }

        if (!channel.canTalk()) {
            println("Does not have permission to post to the review channel.")
        }

        val message = event.message.contentStripped.trim()
        val simplifiedMessage = simplifyString(message)
        val substitutedMessage = performSubstitution(simplifiedMessage)

        when {
            this.wordRegex.containsMatchIn(message) -> {
                val matched = this.wordRegex.find(message)?.groups?.get(0).toString()
                sendFilteredReviewRequest(event, channel, message, matched, "basic")
            }

            this.wordRegex.containsMatchIn(simplifiedMessage) -> {
                this.wordRegex.find(simplifiedMessage)?.groupValues
                val matched = this.wordRegex.find(simplifiedMessage)?.groups?.get(0).toString()
                sendFilteredReviewRequest(event, channel, message, matched, "normalised")
            }

            this.wordRegex.containsMatchIn(substitutedMessage) -> {
                val matched = this.wordRegex.find(substitutedMessage)?.groups?.get(0).toString()
                sendFilteredReviewRequest(event, channel, message, matched, "common substitution")
            }
        }
    }

    override fun onGenericGuildMessageReaction(event: GenericGuildMessageReactionEvent) {
        if (event.channel.name != config.flaggedForReviewChannel) {
            return
        }

        val channel = event.guild.textChannels.find { it.name == config.commandChannel }

        if (channel == null) {
            println("Command Channel does not exist... Ignoring")
            return
        }

        println("Got reaction: ${event.reaction.reactionEmote.name ?: "Null event?"}")

        if (event.reaction.reactionEmote.name == deleteSymbol) {
            deleteEvent(event, deleteSymbol) { message, reason ->
                sendMessage(generateMuteCommand(config.warnCommand, message, 15, reason), channel)
            }
        }

        if (event.reaction.reactionEmote.name == mute15) {
            deleteEvent(event, mute15) { message, reason ->
                sendMessage(generateMuteCommand(config.muteCommand, message, 15, reason), channel)
            }
        }

        if (event.reaction.reactionEmote.name == mute30) {
            deleteEvent(event, mute30) { message, reason ->
                sendMessage(generateMuteCommand(config.muteCommand, message, 30, reason), channel)
            }
        }

        if (event.reaction.reactionEmote.name == mute45) {
            deleteEvent(event, mute45) { message, reason ->
                sendMessage(generateMuteCommand(config.muteCommand, message, 45, reason), channel)
            }
        }

        if (event.reaction.reactionEmote.name == mute60) {
            deleteEvent(event, mute60) { message, reason ->
                sendMessage(generateMuteCommand(config.muteCommand, message, 60, reason), channel)
            }
        }

        if (event.reaction.reactionEmote.name == keepSymbol) {
            val message = event.channel.getMessageById(event.messageId).complete()
            val count = message.reactions
                    .filter { it.reactionEmote.name == keepSymbol }
                    .map { it.count }.singleOrNull() ?: 0

            if (count <= 1) {
                return
            }

            event.channel.getMessageById(event.messageId)
                    .queue { it.delete().queue() }

            message.delete().queue()
        }
    }

    private fun sendFilteredReviewRequest(event: MessageReceivedEvent, channel: MessageChannel, message: String, matchedPhrase: String, filter: String) {
        val embed = EmbedBuilder()
                .setTitle("You might want to check this one")
                .setDescription("Triggered the $filter filter:\n$message")
                .setColor(Color.RED)
                .addField("matched phrase", matchedPhrase, true)
                .addField("message", event.messageId, true)
                .addField("channel", event.channel.id, true)
                .setFooter("Triggered by ${event.member.effectiveName}.", null)
                .build()

        channel.sendMessage(embed)
                .submit()
                .thenAccept {
                    it.addReaction(keepSymbol).queue()
                    it.addReaction(deleteSymbol).queue()
                    it.addReaction(mute15).queue()
                    it.addReaction(mute30).queue()
                    it.addReaction(mute45).queue()
                    it.addReaction(mute60).queue()
                }
    }

    private fun generateMuteCommand(command: String, message: Message, minutes: Int, reason: String): String {
        return command
                .replace("{{name}}", message.author.name)
                .replace("{{minutes}}", minutes.toString())
                .replace("{{reason}}", reason)
    }

    private fun deleteEvent(event: GenericGuildMessageReactionEvent, emoji: String, callback: ((Message, String) -> Unit)? = null) {
        val message = event.channel.getMessageById(event.messageId).complete()
        val count = message.reactions
                .filter { it.reactionEmote.name == emoji }
                .map { it.count }.singleOrNull() ?: 0

        if (count <= 1) {
            return
        }

        if (message.embeds.size == 0) {
            return
        }

        val embed = message.embeds[0]
        val messageField = embed.fields.filter { it.name == "message" }.single()
        val channelField = embed.fields.filter { it.name == "channel" }.single()

        val channel = event.guild.getTextChannelById(channelField.value)

        channel.getMessageById(messageField.value).queue {
            val guild = it.guild
            val selfMember = guild.selfMember

            val reason = "Message triggered the filter and was deleted by: ${it.member.effectiveName}"

            if (callback != null) {
                callback(message, reason)
            }

            if (selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {

                it.delete().reason(reason).queue()
                sendEmbed("Filtered Message", reason, event, Color.GREEN)
            } else {
                sendEmbed("Filtered Message", "Could not delete message... Insufficient permissions.", event, Color.RED)
            }
        }

        message.delete().queue()
    }

    val wordRegex: Regex by lazy {
        Regex(config.words.joinToString("|") {
            val output = StringBuilder()

            for (char in it) {
                output.append(char)
                output.append("\\s*")
            }

            output.toString()
        })
    }
}

private fun simplifyString(message: String): String {
    val normalised = Normalizer.normalize(message, Normalizer.Form.NFD)

    val sb = StringBuilder(normalised.length)

    normalised.toCharArray()
            .filter { it <= '\u007F' }
            .forEach { sb.append(it) }

    return sb.toString()
}

private fun performSubstitution(message: String): String {
    return message
            .replace("!", "i")
            .replace("3", "e")
            .replace("1", "i")
            .replace("5", "s")
            .replace("0", "o")
            .replace("$", "s")
            .replace("7", "t")
            .replace("8", "b")
}

data class WordFilterConfig(
        val enabled: Boolean = true,
        val words: List<String> = mutableListOf(),
        val flaggedForReviewChannel: String = "review-required",
        val commandChannel: String = "talk-to-the-boop",
        val filteredChannels: List<String> = mutableListOf(),
        val muteCommand: String = "!mute @{{name}} {{minutes}} {{reason}}",
        val warnCommand: String = "!warn @{{name}} {{minutes}} {{reason}}"
)