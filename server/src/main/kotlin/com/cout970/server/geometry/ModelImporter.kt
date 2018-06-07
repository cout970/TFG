package com.cout970.server.geometry

import com.cout970.server.scene.DGeometry
import java.io.File

object ModelImporter {

    fun import(path: String): DGeometry {
        val file = File(path)

        check(file.exists()) { "File does not exist: $path, absolute: ${file.absolutePath}" }

        return when (file.extension) {
            "obj" -> ObjImporter.import(file, false)
//            "gltf" -> importGltfModel(file)
            else -> error("Unknown model type: ${file.extension} of file: $path")
        }
    }
}