package com.cout970.server.rest

import org.joml.Vector2ic


class HeightMap(val map: FloatArray, val width: Int, val height: Int) {

    fun getOrNull(x: Int, y: Int): Float? = if (x in 0..(width - 1) && y in 0..(height - 1)) map[x + y * width] else null

    operator fun get(x: Int, y: Int): Float = map[x + y * width]

    operator fun set(x: Int, y: Int, value: Float) {
        map[x + y * width] = value
    }
}

fun heightMapOfSize(width: Int, height: Int) = HeightMap(FloatArray(width * height), width, height)

data class Chunk(
        val pos: Vector2ic,
        val heights: HeightMap
) {
    lateinit var cache: Pair<String, String>
}