package com.cout970.server.rest

import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.debug
import com.cout970.server.util.info
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import spark.Request
import spark.kotlin.ignite
import java.io.File
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


object Rest {

    private val gson = GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create()

    val cacheMap = Collections.synchronizedMap(mutableMapOf<String, FloatArray>())

    private val sceneRegistry = mutableMapOf<String, String>()

    fun httpServer() {

        loadScenes()

        ignite().apply {
            port(8080)

            // get file
            get("/api/files/:filename") {
                File("files/${request.params("filename")}").readBytes()
            }

            get("/api/scene/:filename") {
                val filename = request.params("filename")

                if (filename in sceneRegistry) {
                    File("files/${sceneRegistry[filename]}").readBytes()
                } else {
                    response.status(404)
                    response.body("Requested scene doesn't exist")
                    Unit
                }
            }

            get("/api/scenes") {
                sceneRegistry.entries.joinToString(",", prefix = "[", postfix = "]") { "\"${it.key}\"" }
            }

            post("/api/scenes") {
                val scene = SCENE_GSON.fromJson(request.body(), DScene::class.java)
                val name = registerScene(scene)
                response.header("location", name)
            }

            // scenes
            get("/api/scene/:id") {
                val writer = JsonWriter(OutputStreamWriter(this.response.raw().outputStream, "UTF-8"))
                debug("Loading scene")
                gson.toJson(scene, DScene::class.java, writer)
                writer.close()
                ""
            }

            get("/api/binary/:id") {
                val id = request.params("id")

                debug("Loading binary blob: $id")

                val array = cacheMap[id] ?: return@get ""

                response.type("application/octet-stream")
                val bytes = ByteArray(array.size * 4)

                ByteBuffer
                        .wrap(bytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asFloatBuffer()
                        .put(array)

                this.response.raw().outputStream.write(bytes)
                ""
            }
        }
    }

    fun registerScene(scene: DScene): String {
        val name = UUID.randomUUID().toString()
        val (header, buffer) = SceneBaker.bake(scene, "$name.bin")

        File("files/$name.gltf").writeText(GLTF_GSON.toJson(header))
        File("files/$name.bin").writeBytes(buffer)

        sceneRegistry[name] = "$name.gltf"
        saveScenes()
        return name
    }

    private fun loadScenes() {
        val json = File("scenes.json")
        sceneRegistry.clear()

        try {
            if (json.exists()) {
                val obj = JsonParser().parse(json.reader())

                obj.asJsonObject.entrySet().forEach { (key, value) ->
                    sceneRegistry[key] = value.asString
                }
                info("Loaded scene.json")
            } else {
                info("File scene.json not found, creating...")
                saveScenes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveScenes() {
        val json = File("scenes.json")
        json.writeText(gson.toJson(sceneRegistry))
        info("Saved scene.json")
    }

    private fun parseVector2(request: Request): Pair<Int, Int> {
        val xStr = request.params("x")
        val yStr = request.params("y")

        val x = xStr.toIntOrNull() ?: throw IllegalStateException("x must be an integer, but was: $xStr")
        val y = yStr.toIntOrNull() ?: throw IllegalStateException("y must be an integer, but was: $yStr")

        return x to y
    }
}