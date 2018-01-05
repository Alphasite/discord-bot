
import bots.*
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.exceptions.RateLimitedException
import java.nio.file.Files
import java.nio.file.Paths
import javax.security.auth.login.LoginException

class BotRoot(val token: String): Runnable {
    override fun run() {
        try {
            val configRoot = Paths.get("config")

            if (!Files.exists(configRoot)) {
                Files.createDirectories(configRoot)
            }

            val bots = arrayOf<Bot>(
                    VoteBot(configRoot),
                    NameBot(configRoot),
                    RoleBot(configRoot),
                    WordFilterBot(configRoot)
            )

            val botBuilder = JDABuilder(AccountType.BOT)
                    .setToken(token)

            botBuilder.addEventListener(HelpBot(configRoot, bots))

            for (bot in bots) {
                botBuilder.addEventListener(bot)
            }

            val jda = botBuilder.buildBlocking()

        } catch (e: LoginException) {
            println("Error logging in.")
        } catch (e: InterruptedException) {
            println("Interrupted.")
        } catch (e: RateLimitedException) {
            println("Hit rate limit... dead.")
        }
    }
}