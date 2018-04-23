package com.cout970.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.cout970.server.ddbb.DDBBManager
import com.cout970.server.ddbb.ShapeDAO
import com.cout970.server.rest.Rest.httpServer
import com.cout970.server.rest.bakeScene
import com.cout970.server.util.TerrainLoader
import com.cout970.server.util.TerrainLoader.bakeTerrain
import com.cout970.server.util.ifFail
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis


fun main(args: Array<String>) {

    println("Starting...")
    val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    root.level = Level.INFO

    Locale.setDefault(Locale.US)

    println("Starting DDBB connection")
    DDBBManager::init.ifFail {
        println("Error: DDBB connection")
        return
    }
    println("Done: DDBB connection")

    println("Loading height maps")
    var error = false
    var time: Long

    time = measureTimeMillis {
        error = TerrainLoader.loadHeightMaps()
        bakeTerrain()
    }
    if (error) println("Done: Map loading ($time ms)") else println("Exception in Map loading ($time ms)")

    System.gc()

    println("Loading buildings")
    time = measureTimeMillis {
        ShapeDAO.loadBuildings()
    }
    println("Buildings loaded ($time ms)")

    System.gc()

    println("Baking scene")
    time = measureTimeMillis {
        bakeScene()
    }
    println("Scene baked ($time ms)")

    System.gc()

    println("Starting: http server")
    httpServer()
    println("Done: http server")
}