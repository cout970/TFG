package com.cout970.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.cout970.server.ddbb.DDBBManager
import com.cout970.server.rest.Rest.httpServer
import com.cout970.server.scene.bakeScene
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.FontExtrude
import com.cout970.server.util.info
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

val jsEngine: ScriptEngine = ScriptEngineManager().getEngineByExtension("js")

fun main(args: Array<String>) {

    info("Starting...")
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.INFO

    Locale.setDefault(Locale.US)

    info("Starting DDBB connection")
    try {
        DDBBManager.init()
    } catch (e: Exception) {
        info("Error: DDBB connection")
        e.printStackTrace()
        return
    }
    info("Done: DDBB connection")

    info("Loading THREE")
    jsEngine.eval(Thread.currentThread().contextClassLoader.getResourceAsStream("three.min.js").reader())
    FontExtrude.init()
    info("Done: THREE")

    info("Loading height maps")
    var error = false
    var time: Long

    time = measureTimeMillis {
        error = TerrainLoader.loadHeightMaps()
    }
    when (error) {
        true -> info("Exception in Map loading ($time ms)")
        else -> info("Done: Map loading ($time ms)")
    }

    System.gc()

    // clear old scenes
    File("files").listFiles()?.forEach {
        it.delete()
    }

    info("Baking scene")
    time = measureTimeMillis {
        bakeScene()
    }
    info("Scene baked ($time ms)")

    System.gc()

    info("Starting: http server")
    httpServer()
    info("Done: http server")
}