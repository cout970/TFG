package com.cout970.server.util

import com.cout970.server.rest.TerrainLoader


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