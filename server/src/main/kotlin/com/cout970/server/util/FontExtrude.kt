package com.cout970.server.util

import com.cout970.server.jsEngine
import com.cout970.server.scene.DBufferGeometry
import com.cout970.server.scene.GeometryBuilder
import jdk.nashorn.api.scripting.ScriptObjectMirror
import java.io.File

object FontExtrude {

    fun init() {
        jsEngine.eval("""
            var fontLoader = new THREE.FontLoader();
            var typeface = ${File("../client/src/style/fonts/Roboto_Regular.json").readText()};
            var default_font = fontLoader.parse(typeface);
        """.trimIndent())
    }

    @Suppress("UNCHECKED_CAST")
    fun extrudeLabel(txt: String, scale: Double): DBufferGeometry {

        val geometry: ScriptObjectMirror = synchronized(FontExtrude) {
            jsEngine.eval("""
                var geometry = new THREE.TextGeometry("${txt.replace("\"", "\\\"")}", {
                    font: default_font,
                    size: $scale,
                    height: 1,
                    curveSegments: 4,
                    bevelEnabled: false,
                    bevelThickness: 10,
                    bevelSize: 8
                });

                var bufferGeometry = new THREE.BufferGeometry();
                bufferGeometry.fromGeometry(geometry);
                bufferGeometry.attributes.position.array
            """.trimIndent()) as ScriptObjectMirror
        }

        val array = geometry.values as List<Double>

        return GeometryBuilder.build(array.map { it.toFloat() })
    }
}