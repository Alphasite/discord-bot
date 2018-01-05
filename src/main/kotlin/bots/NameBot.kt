package bots

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import sendFailureEmbed
import sendPermissionsFailureEmbed
import sendSuccessEmbed
import splitOnWhiteSpace
import java.nio.file.Path

class NameBot(configRoot: Path) : BotImpl(configRoot) {
    val config = Bot.loadConfig(configPath, NameConfig())

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val guild = event.message.guild
        val selfMember = guild.selfMember

        if (event.message.mentionedUsers.contains(selfMember.user)) {
            val message = event.message.contentStripped.trim()
            val segments = message.splitOnWhiteSpace()

            when (segments[1]) {
                "$path.set" -> {
                    if (event.message.mentionedUsers.size != 2) {
                        sendFailureEmbed("Did not mention user", "You need to mention someone to have them renamed.", event, false)
                    }

                    val targetUser = event.message.mentionedUsers[1]
                    var targetName = segments[3]

                    if (targetName.startsWith("\"")) {
                        targetName = targetName.drop(1).dropLast(1)
                    }

                    handleNameChangePermission(targetUser, event) { member ->
                        guild.controller.setNickname(member, targetName).queue({
                            sendSuccessEmbed("Nickname Changed", "The user ${member.user.name} was renamed.", event)
                        }, {
                            sendFailureEmbed("Failed to Change Nickname", "Could not rename ${member.user.name}.", event)
                        })
                    }
                }
            }

            event.message.delete().queue()
        }
    }

    override val path get() = "name"

    override val help: List<String> get() {
        return listOf(
                "$path.set @<user> <name>: set new name for user"
        )
    }

    fun handleNameChangePermission(
            targetUser: User,
            event: MessageReceivedEvent,
            action: (Member) -> Unit
    ) {
        val guild = event.message.guild
        val selfMember = guild.selfMember

        if (!selfMember.hasPermission(Permission.NICKNAME_CHANGE)) {
            sendFailureEmbed("Insufficient Permissions", "Bot doesn't have the permission to change nicknames...", event)
        }

        val targetMember = guild.getMember(targetUser)

        if (!selfMember.canInteract(targetMember)) {
            sendFailureEmbed("Insufficient Permissions", "Bot doesn't have the permission to change this users nickname.", event)
        }

        try {
            action(targetMember)
        } catch (e: PermissionException) {
            sendPermissionsFailureEmbed(event)
        }
    }
}

data class NameConfig(val enabled: Boolean = true)