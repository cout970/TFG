package com.cout970.server.util

import com.cout970.server.rest.Chunk
import com.cout970.server.rest.heightMapOfSize
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.joml.Vector2i
import org.joml.Vector3f
import java.awt.image.RenderedImage
import java.io.File
import kotlin.math.max
import kotlin.math.min

object TerrainLoader {

    val ORIGIN = Vector3f(538973.6319697625f, 0f, 4750077.070013605f)
    private val envelope = Vector3f()
    val terrainLevel = mutableMapOf<Pair<Int, Int>, Chunk>()
    val center = Vector2i()

    val CHUNK_PIXELS = 256
    val PIXEL_SIZE = 25 // meters

    fun relativize(coords: List<Double>): List<Float> {
        val result = mutableListOf<Float>()

        repeat(coords.size / 3) {
            val x = coords[it * 3].toFloat()
            val z = coords[it * 3 + 1].toFloat()
            val y = coords[it * 3 + 2].toFloat()

            result += x - ORIGIN.x
            result += y //+ getHeight(x, y)
            result += z - ORIGIN.z
        }
        return result
    }

    fun getHeight(x: Float, y: Float): Float {
        val scale = CHUNK_PIXELS * PIXEL_SIZE
        val absX = x - envelope.x
        val absY = y - envelope.z
        val pos = absX.toInt() / scale to absY.toInt() / scale
        val key = pos.first + center.x to pos.second + center.y
        val chunk: Chunk = terrainLevel[key] ?: return 0f
        val gridX = (chunk.heights.width * (absX - pos.first * scale) / scale).toInt()
        val gridY = (chunk.heights.height * (absY - pos.second * scale) / scale).toInt()
        return chunk.heights[gridX, gridY]
    }

    private fun loadLevel1Map() {
        val file = File("GaliciaDTM25m.tif")
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage
        val pos = coverage.envelope2D.minX.toFloat() to coverage.envelope2D.minY.toFloat()

        envelope.x = pos.first
        envelope.z = pos.second
        terrainLevel += rasterize(image, pos, CHUNK_PIXELS, PIXEL_SIZE.toFloat())
    }

    fun rasterize(image: RenderedImage, pos: Pair<Float, Float>, pixelsPerChunk: Int, meters: Float)
            : Map<Pair<Int, Int>, Chunk> {

        val data = image.data
        val xRange = 0 until image.width
        val yRange = 0 until image.height

        val map = mutableMapOf<Pair<Int, Int>, Chunk>()

        var max = 0
        val sizeX = Math.ceil(image.width / pixelsPerChunk.toDouble()).toInt()
        val sizeY = Math.ceil(image.height / pixelsPerChunk.toDouble()).toInt()
        val xRangeIter = 0..sizeX
        val yRangeIter = 0..sizeY

        center.set(-sizeX / 2, -sizeY / 2)

        xRangeIter.toList().parallelStream().map { x ->
            yRangeIter.mapNotNull { y ->

                val start = Vector3f(pos.first + x * pixelsPerChunk * meters, 0f, pos.second + y * pixelsPerChunk * meters)

                if (ORIGIN.distance(start) < 100000f) {

                    val a = (ORIGIN.distance(start) / 100000f)
                    val quality = -Math.log(a + 0.1)
                    val vertexPerChunk = max(8, min(pixelsPerChunk, nearestPowerOf2((quality * pixelsPerChunk).toInt())))

                    val relScale = (pixelsPerChunk / vertexPerChunk)
                    val heightMap = heightMapOfSize(vertexPerChunk + 1, vertexPerChunk + 1)

                    for (i in 0..vertexPerChunk) {
                        for (j in 0..vertexPerChunk) {

                            val absX = x * pixelsPerChunk + i * relScale
                            val absY = y * pixelsPerChunk + j * relScale

                            if (absX in xRange && absY in yRange) {
                                val pixel = data.getSample(absX, absY, 0)
                                if (pixel > 0) {
                                    heightMap[i, j] = pixel.toFloat()
                                    max = Math.max(max, pixel)
                                }
                            }
                        }
                    }
                    val posX = pos.first + x * pixelsPerChunk * meters - ORIGIN.x
                    val posY = pos.second + y * pixelsPerChunk * meters - ORIGIN.z
                    val chunk = Chunk(posX, posY, heightMap, 0f, meters * pixelsPerChunk)

                    (x + center.x to y + center.y) to chunk
                } else null
            }
        }.forEach { it.forEach { map += it } }

        map.values.forEach { it.maxHeight = max.toFloat() }

        println("Chunks in map: ${map.size}")
        return map
    }

    private fun nearestPowerOf2(a: Int): Int {
        var x = a - 1
        x = x or (x shr 1)
        x = x or (x shr 2)
        x = x or (x shr 4)
        x = x or (x shr 8)
        x = x or (x shr 16)
        return x + 1
    }

    fun loadHeightMaps(): Boolean {
        var error = false
        try {
            loadLevel1Map()
        } catch (e: Exception) {
            println("Error loading level 0: $e")
            error = true
        }
        return error
    }
}