package com.cout970.server.rest

import org.joml.Vector3f

object MeshBuilder {

    fun chunkToModel(chunk: Chunk): Defs.Geometry {
        return heightMapToModel(chunk.heights,
                Vector3f(chunk.posX, 0f, chunk.posY),
                Vector3f(chunk.scale, 1f, chunk.scale)
        )
    }

    fun heightMapToModel(map: HeightMap, offset: Vector3f, scale: Vector3f): Defs.Geometry {

        val low = Vector3f(0f, 1f, 0f)
        val high = Vector3f(0x22 / 255f, 0x20 / 255f, 0x1E / 255f)
        val blue = Vector3f(0f, 0.5f, 1f)

        val size = map.width * map.height * 6 * 3
        val vertexData = FloatArray(size)
        val colorData = FloatArray(size)
        var ptr = 0
        var ptr2 = 0

        fun append(i: Int, j: Int) {
            val height = map[i, j]

            vertexData[ptr++] = offset.x + (scale.x * i / (map.width - 1))
            vertexData[ptr++] = offset.y + scale.y * height
            vertexData[ptr++] = offset.z + (scale.z * j / (map.height - 1))

            val color = if (height == 0f) blue else Vector3f(low).lerp(high, height / 2000)
            colorData[ptr2++] = color.x
            colorData[ptr2++] = color.y
            colorData[ptr2++] = color.z
        }

        for (i in 0 until map.width - 1) {
            for (j in 0 until map.height - 1) {
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