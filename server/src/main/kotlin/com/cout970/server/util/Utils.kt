package com.cout970.server.util

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Shape.BakedShape
import eu.printingin3d.javascad.coords.Coords3d


inline fun (() -> Unit).ifFail(func: () -> Unit) {
    try {
        this()
    } catch (e: Exception) {
        e.printStackTrace()
        func()
    }
}

fun earthToScene(pos: Pair<Float, Float>): Pair<Float, Float> {
    val minX = TerrainLoader.ORIGIN.x
    val minY = TerrainLoader.ORIGIN.z

    return -(pos.first - minX) to (pos.second - minY)
}

fun Defs.Geometry.merge(other: Defs.Geometry): Defs.Geometry {
    val attrMap = mutableMapOf<String, Defs.BufferAttribute>()

    attributes.forEach {
        attrMap[it.attributeName] = it
    }

    other.attributes.forEach {
        if (it.attributeName in attrMap) {
            attrMap[it.attributeName] = it.merge(attrMap.getValue(it.attributeName))
        } else {
            attrMap[it.attributeName] = it
        }
    }

    return Defs.Geometry(attrMap.values.toList())
}

fun Defs.BufferAttribute.merge(other: Defs.BufferAttribute): Defs.BufferAttribute {
    return Defs.BufferAttribute(attributeName, data + other.data, count)
}

fun Polygon.flip(): Polygon {
    return Polygon(points.reversed())
}

fun List<Coords3d>.center(): Coords3d {
    if (isEmpty()) return Coords3d.ZERO

    var x = 0.0
    var y = 0.0
    var z = 0.0

    forEach {
        x += it.x
        y += it.y
        z += it.z
    }

    return Coords3d(
            x / size,
            y / size,
            z / size
    )
}

fun BakedShape.merge(other: BakedShape): BakedShape {
    val geometry = this.model.geometry.merge(other.model.geometry)
    return BakedShape(Defs.Model(geometry, this.model.material))
}