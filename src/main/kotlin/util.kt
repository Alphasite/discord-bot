
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil
import java.awt.Color
import java.util.concurrent.TimeUnit

fun sendMessage(body: String, channel: MessageChannel) {
    if (body.isNotBlank()) {
        channel.sendMessage(body).queue()
        println("Sent message: $body")
    }
}

fun sendEmbed(title: String, description: String, sourceEvent: GenericGuildMessageReactionEvent, colour: Color, log: Boolean = true, delayMinutes: Long = 1) {
    val embed = EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(colour)
            .setFooter("Response to ${sourceEvent.member.effectiveName}", null)
            .build()

    val logChannel = sourceEvent.guild.getTextChannelsByName("log", true)[0]
    val eventChannel = sourceEvent.channel

    if (log) {
        logChannel.sendMessage(embed).queue()
    }

    eventChannel.sendMessage(embed).queue({
        println("Message sent: $embed")
        it.delete().queueAfter(delayMinutes, TimeUnit.MINUTES)
    }, {
        println("Message Failed to send: $embed")
    })
}


fun sendEmbed(title: String, description: String, sourceEvent: MessageReceivedEvent, colour: Color, log: Boolean = true, delayMinutes: Long = 1) {
    val embed = EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(colour)
            .setFooter("Response to ${sourceEvent.member.effectiveName}: ${sourceEvent.message.content}", null)
            .build()

    val logChannel = sourceEvent.guild.getTextChannelsByName("log", true)[0]
    val eventChannel = sourceEvent.channel

    if (log) {
        logChannel.sendMessage(embed).queue()
    }

    eventChannel.sendMessage(embed).queue({
        println("Message sent: $embed")
        it.delete().queueAfter(delayMinutes, TimeUnit.MINUTES)
    }, {
        println("Message Failed to send: $embed")
    })
}

public fun sendSuccessEmbed(title: String, description: String, sourceEvent: MessageReceivedEvent, log: Boolean = true) {
    sendEmbed(title, description, sourceEvent, Color.green, log)
}

public fun sendFailureEmbed(title: String, description: String, sourceEvent: MessageReceivedEvent, log: Boolean = true) {
    sendEmbed(title, description, sourceEvent, Color.red, log)
}

public fun sendPermissionsFailureEmbed(event: MessageReceivedEvent) {
    sendFailureEmbed("Permission Denied", "I don't have sufficient seniority for that permission senior.", event)
}

public fun handleRolePermission(
        role: String,
        event: MessageReceivedEvent,
        action: (Role) -> Unit
) {
    val guild = event.message.guild
    val selfMember = guild.selfMember

    val matching_roles = guild.getRolesByName(role, true)

    when (matching_roles.size) {
        0 -> {
            sendFailureEmbed("No Roles Match", "There is no role with that name.", event)
        }
        1 -> {
            val matching_role = matching_roles[0]

            if (validatePermission(guild, selfMember, matching_role)) {
                try {
                    action(matching_role)
                } catch (e: PermissionException) {
                    sendPermissionsFailureEmbed(event)
                }
            } else {
                sendPermissionsFailureEmbed(event)
            }
        }
        else -> {
            sendFailureEmbed("Multiple Roles Match", "Multiple roles match the name.", event)
        }
    }
}

public fun validatePermission(guild: Guild, self: Member, role: Role): Boolean {
    val lower_roles = guild.roles
            .dropWhile { it != self.roles.last() }
            .drop(1)

    return lower_roles.contains(role) && PermissionUtil.canInteract(self, role)
}

val stringSplitRegex = "\"?( |$)(?=(([^\"]*\"){2})*[^\"]*$)\"?".toRegex()

public fun String.splitOnWhiteSpace(): List<String> {
    return this.trim().split(stringSplitRegex)
}