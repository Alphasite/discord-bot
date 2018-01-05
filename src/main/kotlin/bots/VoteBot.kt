package bots

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.nio.file.Path

class VoteBot(configRoot: Path) : BotImpl(configRoot) {
    private val deleteSymbol = "❎"
    private val config = Bot.loadConfig(configPath, VoteConfig())

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        super.onMessageReactionAdd(event)

        println("Got reaction: ${event.reaction.emote.name ?: "Null event?"}")

        if (event.reaction.emote.name == deleteSymbol) {
            val message = event.channel.getMessageById(event.messageId).complete()
            val delete = message.reactions.find { it.emote.name == deleteSymbol }
            val guild = message.guild
            val selfMember  = guild.selfMember
            val messageMember = guild.getMemberById(message.author.id)

            if (delete != null && delete.count >= 1) {
                if (selfMember.hasPermission(Permission.MESSAGE_MANAGE) && selfMember.canInteract(messageMember)) {
                    message.delete().queue()
                    println("Deleting message '${message.strippedContent}' due to requests.")
                } else {
                    println("Could not delete message... Insufficient permissions.")
                }
            } else {
                println("No delete emotes.")
            }
        }
    }

    override val path get() = "vote"

    override val help: List<String> get() {
        return listOf()
    }
}

public data class VoteConfig(
        val enabled: Boolean = true
)

