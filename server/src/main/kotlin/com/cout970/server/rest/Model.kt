package com.cout970.server.rest

import org.joml.Vector3f

// TODO remove

enum class ShapeType { LINE, MESH, POLYGONS }

data class Shape(val indices: List<Int>)

data class Model(val vertex: List<Vector3f>, val shapes: List<Shape>, val type: ShapeType)





