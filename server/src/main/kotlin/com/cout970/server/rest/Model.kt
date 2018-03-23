package com.cout970.server.rest

import org.joml.Vector3f

enum class ShapeType { LINE, MESH, POLYGONS }

data class Shape(val indices: List<Int>)

data class Model(val vertex: List<Vector3f>, val shapes: List<Shape>, val type: ShapeType)


data class Chunk(
        val posX: Float,
        val posY: Float,
        val heights: HeightMap,
        val maxHeight: Float,
        val size: Int
)