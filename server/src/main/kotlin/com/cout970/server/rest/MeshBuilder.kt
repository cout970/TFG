package com.cout970.server.rest

import org.joml.Vector3f
import kotlin.math.min
import kotlin.math.tanh

object MeshBuilder {

    fun buildGeometry(indices: List<Int>, coords: List<Float>): Defs.Geometry {
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

        return Defs.Geometry(listOf(
                Defs.BufferAttribute("position", vertexData, 3),
                Defs.BufferAttribute("color", colorData, 3)
        ))
    }

    fun chunkToModel(chunk: Chunk): Defs.Geometry {
        return heightMapToModel(chunk.heights, 8,
                Vector3f(chunk.posX, 0f, chunk.posY),
                Vector3f(chunk.scale, 1f, chunk.scale)
        )
    }

    fun heightMapToModel(map: HeightMap, quality: Int, offset: Vector3f, scale: Vector3f): Defs.Geometry {

        val low = Vector3f(0f, 1f, 0f)
        val high = Vector3f(0x22 / 255f, 0x20 / 255f, 0x1E / 255f)
        val blue = Vector3f(0f, 0.5f, 1f)

        val size = quality * quality * 6 * 3
        val vertexData = FloatArray(size)
        val colorData = FloatArray(size)
        var ptr = 0
        var ptr2 = 0

        val iter = min(quality, min(map.width - 1, map.height - 1))

        fun append(i: Int, j: Int) {
            val mapX = i * map.width / (iter + 1)
            val mapY = j * map.height / (iter + 1)
            val height = map[mapX, mapY]

            vertexData[ptr++] = offset.x + (scale.x * i / iter)
            vertexData[ptr++] = offset.y + scale.y * height
            vertexData[ptr++] = offset.z + (scale.z * j / iter)

            val color = if (height == 0f) blue else Vector3f(low).lerp(high, height / 2000)
            colorData[ptr2++] = color.x
            colorData[ptr2++] = color.y
            colorData[ptr2++] = color.z
        }

        for (i in 0 until iter) {
            for (j in 0 until iter) {
                append(i, j)
                append(i, j + 1)
                append(i + 1, j + 1)

                append(i, j)
                append(i + 1, j + 1)
                append(i + 1, j)
            }
        }
        return Defs.Geometry(listOf(
                Defs.BufferAttribute("position", vertexData, 3),
                Defs.BufferAttribute("color", colorData, 3)
        ))
    }
}