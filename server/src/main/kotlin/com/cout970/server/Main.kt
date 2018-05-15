package com.cout970.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.cout970.server.ddbb.DDBBManager
import com.cout970.server.rest.Rest.httpServer
import com.cout970.server.rest.bakeScene
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.terrain.TerrainLoader.bakeTerrain
import com.cout970.server.util.ifFail
import com.cout970.server.util.info
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {

    info("Starting...")
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.INFO

    Locale.setDefault(Locale.US)

    info("Starting DDBB connection")
    DDBBManager::init.ifFail {
        info("Error: DDBB connection")
        return
    }
    info("Done: DDBB connection")

    info("Loading height maps")
    var error = false
    var time: Long

    time = measureTimeMillis {
        error = TerrainLoader.loadHeightMaps()
        bakeTerrain()
    }
    if (error) info("Done: Map loading ($time ms)") else info("Exception in Map loading ($time ms)")

    System.gc()

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