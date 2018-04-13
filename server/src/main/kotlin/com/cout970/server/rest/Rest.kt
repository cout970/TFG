package com.cout970.server.rest

import com.cout970.server.util.MeshBuilder
import com.cout970.server.util.TerrainLoader.terrainLevel
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import spark.Request
import spark.kotlin.ignite
import java.io.File
import java.io.OutputStreamWriter

object Rest {

    private val gson = GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create()

    fun httpServer() {

        ignite().apply {
            port(8080)

            // web index
            get("/") {
                File("core-js/web/index.html").readText()
            }

            // terrain
            get("/api/height/:x/:y") {
                val (x, y) = parseVector2(request)
                val map = terrainLevel[x to y] ?: return@get "{ \"error\": \"No map\" }"

                System.gc()
                val geom = MeshBuilder.chunkToModel(map)
                gson.toJson(geom).apply {
                    System.gc()
                }
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

            // scenes
            get("/api/scene/:id") {
                val writer = JsonWriter(OutputStreamWriter(this.response.raw().outputStream, "UTF-8"))
                gson.toJson(scene, Defs.Scene::class.java, writer)
                writer.close()
                ""
            }

            //            /// buildings test
//            get("/api/buildings/:x/:y") {
//                gson.toJson(ShapeDAO.getBuildings(parseVector2(request)))
//            }
//
//            get("/api/buildings2/:x/:y") {
//                gson.toJson(ShapeDAO.getBuildingsIn(parseVector2(request)))
//            }
//
//            /// buildings test
//            get("/api/streets/:x/:y") {
//                gson.toJson(ShapeDAO.getStreets(parseVector2(request)))
//            }
        }
    }

    private fun parseVector2(request: Request): Pair<Int, Int> {
        val xStr = request.params("x")
        val yStr = request.params("y")

        val x = xStr.toIntOrNull() ?: throw IllegalStateException("x must be an integer, but was: $xStr")
        val y = yStr.toIntOrNull() ?: throw IllegalStateException("y must be an integer, but was: $yStr")

        return x to y
    }
}