package com.cout970.server.util

import com.cout970.server.rest.Chunk
import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Geometry
import com.cout970.server.rest.HeightMap
import org.joml.Vector3f
import kotlin.math.min
import kotlin.math.tanh

object MeshBuilder {

    fun buildGeometry(coords: List<Float>): Geometry {
        val vertexData = FloatArray(coords.size)
        val colorData = FloatArray(coords.size)
        var ptr = 0
        var ptr2 = 0

        repeat(vertexData.size / 3) {
            vertexData[ptr++] = coords[(it * 3)]
            vertexData[ptr++] = coords[(it * 3) + 1]
            vertexData[ptr++] = coords[(it * 3) + 2]

            colorData[ptr2++] = tanh(coords[(it * 3)]) * 0.5f + 0.5f
            colorData[ptr2++] = tanh(coords[(it * 3) + 1]) * 0.5f + 0.5f
            colorData[ptr2++] = tanh(coords[(it * 3) + 2]) * 0.5f + 0.5f
        }

        return Geometry(listOf(
                Defs.BufferAttribute("position", vertexData, 3),
                Defs.BufferAttribute("color", colorData, 3)
        ))
    }

    fun buildGeometry(indices: List<Int>, coords: List<Float>): Geometry {
        val vertexData = FloatArray(indices.size * 3)
        val colorData = FloatArray(indices.size * 3)
        var ptr = 0
        var ptr2 = 0

        repeat(indices.size) {
            vertexData[ptr++] = coords[(it / 3)]
            vertexData[ptr++] = coords[(it / 3) + 1]
            vertexData[ptr++] = coords[(it / 3) + 2]

            colorData[ptr2++] = tanh(coords[(it / 3)]) * 0.5f + 0.5f
            colorData[ptr2++] = tanh(coords[(it / 3) + 1]) * 0.5f + 0.5f
            colorData[ptr2++] = tanh(coords[(it / 3) + 2]) * 0.5f + 0.5f
        }

        return Geometry(listOf(
                Defs.BufferAttribute("position", vertexData, 3),
                Defs.BufferAttribute("color", colorData, 3)
        ))
    }

    fun chunkToModel(chunk: Chunk): Geometry {
        val dist = Vector3f(chunk.posX, 0f, chunk.posY).distance(Vector3f(0f))

        val scale = when (dist) {
            in 0f..8666f -> 64
            in 6666f..16666f -> 64
            in 16666f..33333f -> 32
            in 33333f..50000f -> 16
            in 50000f..63333f -> 8
            in 63333f..80000f -> 4
            else -> 2
        }

        return heightMapToModel(chunk.heights, scale,
                Vector3f(chunk.posX, 0f, chunk.posY),
                Vector3f(chunk.scale, 1f, chunk.scale)
        )
    }

    fun heightMapToModel(map: HeightMap, quality: Int, offset: Vector3f, scale: Vector3f): Geometry {

        val low = Vector3f(0f, 1f, 0f)
        val high = Vector3f(0x22 / 255f, 0x20 / 255f, 0x1E / 255f)
        val blue = Vector3f(0f, 0.5f, 1f)

        val size = quality * quality * 6 * 3
        val vertexData = FloatArray(size)
        val colorData = FloatArray(size)
        var ptr = 0
        var ptr2 = 0

        val iterSize = min(quality, min(map.width - 1, map.height - 1))

        fun append(i: Int, j: Int) {
            val mapX = i * (map.width - 1) / iterSize
            val mapY = j * (map.height - 1) / iterSize
            val height = map[mapX, mapY]

            vertexData[ptr++] = offset.x + (scale.x * i / iterSize)
            vertexData[ptr++] = offset.y + scale.y * height
            vertexData[ptr++] = offset.z + (scale.z * j / iterSize)

            val color = if (height == 0f) blue else Vector3f(low).lerp(high, height / 2000)
            colorData[ptr2++] = color.x
            colorData[ptr2++] = color.y
            colorData[ptr2++] = color.z
        }

        for (i in 0 until iterSize) {
            for (j in 0 until iterSize) {
                append(i, j)
                append(i, j + 1)
                append(i + 1, j + 1)

                append(i, j)
                append(i + 1, j + 1)
                append(i + 1, j)
            }
        }
        return Geometry(listOf(
                Defs.BufferAttribute("position", vertexData, 3),
                Defs.BufferAttribute("color", colorData, 3)
        ))
    }
}