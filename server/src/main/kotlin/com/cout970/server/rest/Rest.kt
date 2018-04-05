package com.cout970.server.rest

import com.cout970.server.ddbb.ShapeDAO
import com.cout970.server.rest.TerrainLoader.terrainLevel
import com.google.gson.GsonBuilder
import org.joml.Vector3f
import spark.Request
import spark.kotlin.ignite
import java.io.File

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

            get("/api/height/:x/:y") {
                val (x, y) = parseVector2(request)
                val map = terrainLevel[x to y] ?: return@get "{ \"error\": \"No map\" }"

                gson.toJson(MeshBuilder.chunkToModel(map))
            }

            /// buildings test
            get("/api/buildings/:x/:y") {
                gson.toJson(ShapeDAO.getBuildings(parseVector2(request)))
            }

            /// buildings test
            get("/api/streets/:x/:y") {
                gson.toJson(ShapeDAO.getStreets(parseVector2(request)))
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

            // position: -257, 139, 238
            // target:  -259, 116, 242
            get("/api/camera") { gson.toJson(Pair(Vector3f(0f, 800f, 0f), Vector3f(0f, 0f, 0f))) }
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