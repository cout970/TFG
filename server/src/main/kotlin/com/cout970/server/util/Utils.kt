package com.cout970.server.util

import com.cout970.server.rest.Defs


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