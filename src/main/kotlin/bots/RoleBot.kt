package bots

import handleRolePermission
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import sendFailureEmbed
import sendEmbed
import sendSuccessEmbed
import validatePermission
import java.awt.Color
import java.nio.file.Path

class RoleBot(configRoot: Path) : BotImpl(configRoot) {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val guild = event.message.guild
        val selfMember = guild.selfMember
        val sender = event.member

        if (event.message.mentionedUsers.contains(selfMember.user)) {
            var message = event.message.strippedContent
            message = message.split("@\\w+".toRegex())[1].trim()

            when {
                message.startsWith("role.add") -> {
                    val role = message.substringAfter("role.add").trim()

                    handleRolePermission(role, event) { matching_role ->
                        guild.controller.addRolesToMember(sender, matching_role).queue({
                            sendSuccessEmbed("Role Added", "The requested role was added.", event)
                        }, {
                            sendFailureEmbed("Role Addition Failed", "The role was not removed.", event)
                        })
                    }
                }
                message.startsWith("role.remove") -> {
                    val role = message.substringAfter("role.remove").trim()

                    handleRolePermission(role, event) { matching_role ->
                        guild.controller.removeSingleRoleFromMember(sender, matching_role).queue({
                            sendSuccessEmbed("Role Removed", "The requested role was removed.", event)
                        }, {
                            sendFailureEmbed("Role Removal Failed", "The requested role was not removed.", event)
                        })
                    }
                }
                message.startsWith("role.list") -> {
                    val roles = guild.roles
                            .filter { validatePermission(guild, selfMember, it) }
                            .map { "@" + it.name }
                            .dropLast(1)
                            .joinToString(", ")

                    sendEmbed("Assignable Roles", roles, event, Color.white, false)
                }
//                else -> sendFailureEmbed("Wat?", "¿Me no comprende Espanol?", event, false)
            }

            event.message.delete().queue()
        }
    }

    override val path get() = "role"

    override val help: List<String> get() {
        return listOf(
                "$path.add <role name>: add role to self",
                "$path.remove <role name>: remove role from self",
                "$path.list: list all assignable roles"
        )
    }
}