package bots

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import net.dv8tion.jda.core.hooks.ListenerAdapter
import okio.Buffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


interface Bot : EventListener {
    val path: String
    val help: List<String>

    companion object {
        val moshi: Moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        inline fun <reified U> loadConfig(configPath: Path, defaultConfig: U): U {
            val jsonAdapter = moshi.adapter<U>(U::class.java)

            if (!Files.exists(configPath)) {
                writeConfig(configPath, defaultConfig)
            }

            val json = String(Files.readAllBytes(configPath), Charset.forName("UTF-8"))

            val config = jsonAdapter.fromJson(json)!!

            // Force update the config if needed?
            writeConfig(configPath, config)

            return config
        }

        inline fun <reified U> writeConfig(configPath: Path, config: U) {
            val jsonAdapter = moshi.adapter<U>(U::class.java)

            val buffer = Buffer()
            val jsonWriter = JsonWriter.of(buffer)
            jsonWriter.indent = "    "

            jsonAdapter.toJson(jsonWriter, config)

            Files.write(configPath, buffer.readByteArray())
        }
    }
}

abstract class BotImpl(configRoot: Path) : ListenerAdapter(), Bot {
    val configPath = configRoot.resolve("$path.json")
}