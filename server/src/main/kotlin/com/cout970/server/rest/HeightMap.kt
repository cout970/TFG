package com.cout970.server.rest


class HeightMap(val map: MutableList<Float>, val width: Int, val height: Int) {

    operator fun get(x: Int, y: Int): Float = map[x + y * width]

    operator fun set(x: Int, y: Int, value: Float) {
        map[x + y * width] = value
    }
}
