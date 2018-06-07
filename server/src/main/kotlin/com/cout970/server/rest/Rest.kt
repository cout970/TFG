package com.cout970.server.rest

import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.scene.DScene
import com.cout970.server.scene.SceneBaker
import com.cout970.server.serialization.SCENE_GSON
import com.cout970.server.util.info
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import spark.kotlin.ignite
import java.io.File
import java.util.*


object Rest {

    private val CONFIG_GSON = GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .create()

    private val sceneRegistry = mutableMapOf<String, String>()

    fun httpServer() {

        loadScenes()

        ignite().apply {
            port(8080)

            // get file
            get("/api/files/:filename") {
                val name = request.params("filename")
                info("File requested: $name")
                File("files/$name").readBytes()
            }

            get("/api/scene/:filename") {
                val filename = request.params("filename")

                if (filename in sceneRegistry) {
                    info("Scene requested: $filename")
                    File("files/${sceneRegistry[filename]}").readBytes()
                } else {
                    info("Scene requested: $filename, but not found")
                    response.status(404)
                    response.body("Requested scene doesn't exist")
                    Unit
                }
            }

            get("/api/scenes") {
                info("Scene list requested")
                sceneRegistry.entries.joinToString(",", prefix = "[", postfix = "]") { "\"${it.key}\"" }
            }

            post("/api/scenes") {
                info("Scene uploaded")
                val scene = SCENE_GSON.fromJson(request.body(), DScene::class.java)
                val name = registerScene(scene)
                response.header("location", name)
            }
        }
    }

    fun registerScene(scene: DScene): String {
        val name = UUID.randomUUID().toString()
        info("Registering scene: $name")

        val (header, buffer) = SceneBaker.bake(scene, "$name.bin")

        File("files/$name.gltf").writeText(GLTF_GSON.toJson(header))
        File("files/$name.bin").writeBytes(buffer)
        File("files/$name.json").writeText(SCENE_GSON.toJson(scene))

        sceneRegistry[name] = "$name.gltf"
        saveScenes()

        info("Scene registered: " +
                "header size = ${Math.ceil(File("files/$name.gltf").length() / 1000.0)}kB, " +
                "binary size = ${Math.ceil(File("files/$name.bin").length() / 1000.0)}kB, " +
                "definition size = ${Math.ceil(File("files/$name.json").length() / 1000.0)}kB")

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
        json.writeText(CONFIG_GSON.toJson(sceneRegistry))
        info("Saved scene.json")
    }
}