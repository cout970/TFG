
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.slf4j.LoggerFactory
import spark.kotlin.ignite
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong

val REACTOR = newSingleThreadContext("reactor")

val lastModified = AtomicLong()

fun main(args: Array<String>) {

    println("Starting...")
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.INFO

    Locale.setDefault(Locale.US)

    println("Testing DDBB connection")
    DDBBManager::init.ifFail {
        return
    }

    println("Starting http server")
    httpServer()
//    println("Starting reactor")
//    reactor()
}


fun httpServer() {

    val gson = GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create()

    ignite().apply {
        port(8080)


        get("/") {
            File("core-js/web/index.html").readText()
        }

        get("/api/height/:x/:y") {
            val xStr = request.params("x")
            val yStr = request.params("y")

            val x = xStr.toIntOrNull() ?: throw IllegalStateException("x must be an integer, but was: $xStr")
            val y = yStr.toIntOrNull() ?: throw IllegalStateException("y must be an integer, but was: $yStr")

            gson.toJson(generateDebugHeightMap(x, y))
        }

        /// multiline test
        get("/api/multiline/:mun") {
            gson.toJson(ShapeDAO.getMultiLine(request.params("mun")))
        }

        // autoreload
        get("/update") {
            lastModified.get().toString()
        }

        // web
        get("/:file") {
            val path = request.params("file")
            val file = File("core-js/web/$path")
            if (file.exists()) {
                file.readText()
            } else {
                response.status(404)
                "Not found"
            }
        }
    }
}

fun reactor() {
    launch(REACTOR) {
        while (true) {
            val mod = File("core-js/src/main/kotlin")
                              .listFiles()
                              ?.map { it.lastModified() }
                              ?.max() ?: 0L


            if (lastModified.get() != mod) {
                println("[Reactor] recompiling")
                recompileClient()
                println("[Reactor] done")
                lastModified.set(mod)
            }

            delay(500)
        }
    }
}

private fun recompileClient() {
    val process = ProcessBuilder()
            .command("gradlew.bat", "core-js:build")
            .directory(File("."))
            .start()

    val result = process
            .inputStream
            .reader()
            .readLines()
            .takeLast(2)
            .first()

    println(result)
}