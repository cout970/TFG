package com.cout970.server.scene

object GeometryBuilder {

    fun build(coords: List<Float>): DBufferGeometry {
        val vertexData = FloatArray(coords.size)
        var ptr = 0

        repeat(vertexData.size / 3) {
            vertexData[ptr++] = coords[(it * 3)]
            vertexData[ptr++] = coords[(it * 3) + 1]
            vertexData[ptr++] = coords[(it * 3) + 2]
        }

        return DBufferGeometry(listOf(
                BufferAttribute("position", vertexData, 3)
        ))
    }

    fun build(indices: List<Int>, coords: List<Float>): DBufferGeometry {
        val vertexData = FloatArray(indices.size * 3)
        var ptr = 0

        repeat(indices.size) {
            vertexData[ptr++] = coords[(it / 3)]
            vertexData[ptr++] = coords[(it / 3) + 1]
            vertexData[ptr++] = coords[(it / 3) + 2]
        }

        return DBufferGeometry(listOf(
                BufferAttribute("position", vertexData, 3)
        ))
    }
}