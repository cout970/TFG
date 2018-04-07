package com.cout970.server.rest

import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.joml.Vector3f
import java.awt.image.RenderedImage
import java.io.File
import kotlin.math.max
import kotlin.math.min

object TerrainLoader {

    val ORIGIN = Vector3f(538973.6319697625f, 0f, 4750077.070013605f)
    private val envelope = Vector3f()
    val terrainLevel = mutableMapOf<Pair<Int, Int>, Chunk>()

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
        val scale = 512 * 25
        val absX = x - envelope.x
        val absY = y - envelope.z
        val pos = absX.toInt() / scale to absY.toInt() / scale
        val chunk: Chunk = terrainLevel[pos] ?: return 0f
        val gridX = (chunk.heights.width * (absX - pos.first * scale) / scale).toInt()
        val gridY = (chunk.heights.height * (absY - pos.second * scale) / scale).toInt()
        return chunk.heights[gridX, gridY]
    }

    fun loadLevel1Map() {
        val file = File("GaliciaDTM25m.tif")
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage
        val pos = coverage.envelope2D.minX.toFloat() to coverage.envelope2D.minY.toFloat()

        envelope.x = pos.first
        envelope.z = pos.second
        terrainLevel += rasterize(image, pos, 512, 25f)
    }

    fun rasterize(image: RenderedImage, pos: Pair<Float, Float>, pixelsPerChunk: Int, meters: Float)
            : Map<Pair<Int, Int>, Chunk> {

        val data = image.data
        val xRange = 0 until image.width
        val yRange = 0 until image.height

        val map = mutableMapOf<Pair<Int, Int>, Chunk>()

        var max = 0
        for (x in 0..Math.ceil(image.width / pixelsPerChunk.toDouble()).toInt()) {
            for (y in 0..Math.ceil(image.height / pixelsPerChunk.toDouble()).toInt()) {

                val start = Vector3f(pos.first + x * pixelsPerChunk * meters, 0f, pos.second + y * pixelsPerChunk * meters)

                if (ORIGIN.distance(start) < 100000) {

                    val a = (ORIGIN.distance(start) / 100000f)
                    val quality = -Math.log(a + 0.1)
                    val vertexPerChunk = max(8, min(512, nearestPowerOf2((quality * 512).toInt())))

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
                    map += (x to y) to Chunk(posX, posY, heightMap, 0f, meters * pixelsPerChunk)
                }
            }
        }
        map.values.forEach { it.maxHeight = max.toFloat() }

        println("Chunks in map: ${map.size}")
        return map
    }

    fun nearestPowerOf2(a: Int): Int {
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