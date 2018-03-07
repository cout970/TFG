package com.cout970.server
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.cout970.server.ddbb.DDBBManager
import com.cout970.server.rest.Rest.httpServer
import com.cout970.server.util.ifFail
import org.slf4j.LoggerFactory
import java.util.*

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

    println("Starting: http server")
    httpServer()
    println("Done: http server")
}