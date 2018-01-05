package bots

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import sendEmbed
import java.awt.Color
import java.nio.file.Path

class HelpBot(
        configRoot: Path,
        bots: Array<Bot>
) : BotImpl(configRoot) {
    val bots: List<Bot> = listOf(this, *bots)
    val config = Bot.loadConfig(configPath, HelpConfig())

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val guild = event.message.guild
        val selfMember = guild.selfMember
        val sender = event.member

        if (event.message.mentionedUsers.contains(selfMember.user)) {
            var message = event.message.strippedContent
            message = message.split("@\\w+".toRegex())[1].trim()

            when {
                message.startsWith("help") -> {
                    val help = StringBuilder()

                    this.bots
                            .flatMap { it.help }
                            .forEach { help.append("\t- @${selfMember.effectiveName} ").append(it).append("\n") }

                    sendEmbed("Help", "Help:\n$help", event, Color.YELLOW, false)
                }

//                else -> sendFailureEmbed("Wat?", "¿Me no comprende Espanol?", event, false)
            }

            event.message.delete().queue()
        }
    }

    override val path get() = "help"

    override val help: List<String> get() {
        return listOf("help: print this help message");
    }
}

data class HelpConfig(val enabled: Boolean = true)