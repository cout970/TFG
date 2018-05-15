package com.cout970.server.terrain

import com.cout970.server.rest.Chunk
import com.cout970.server.rest.Rest
import com.cout970.server.rest.heightMapOfSize
import com.cout970.server.util.MeshBuilder
import com.cout970.server.util.info
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.joml.Vector2i
import org.joml.Vector3f
import java.awt.image.Raster
import java.awt.image.RenderedImage
import java.io.File
import kotlin.math.max

object TerrainLoader {

    val ORIGIN = Vector3f(535917f, 0f, 4746812f)
    val MAP_OFFSET = Vector3f(54952.625f, 0f, 112591f)
    val envelope = Vector3f()
    val terrainLevel = mutableMapOf<Pair<Int, Int>, Chunk>()
    val center = Vector2i()

    val CHUNK_PIXELS = 256
    val PIXEL_SIZE = 25 //25 // meters

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

        val gridXf = chunk.heights.width * (absX - pos.first * scale) / scale
        val gridYf = chunk.heights.height * (absY - pos.second * scale) / scale

        val gridX = gridXf.toInt()
        val gridY = gridYf.toInt()

        val base = chunk.heights[gridX, gridY]
        val nextX = chunk.heights.getOrNull(gridX + 1, gridY) ?: base
        val nextY = chunk.heights.getOrNull(gridX, gridY + 1) ?: base

        return interpolate(interpolate(base, nextX, gridXf - gridX), interpolate(base, nextY, gridYf - gridY), 0.5f)
    }

    private fun interpolate(a: Float, b: Float, c: Float): Float {
        val mu2 = (1f - Math.cos(c * Math.PI).toFloat()) / 2f
        return (a * (1f - mu2)) + (b * mu2)
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

    private fun rasterize(image: RenderedImage, pos: Pair<Float, Float>, pixelsPerChunk: Int, meters: Float)
            : Map<Pair<Int, Int>, Chunk> {

        val data = image.data
        val xRange = 0 until image.width
        val yRange = 0 until image.height

        val map = mutableMapOf<Pair<Int, Int>, Chunk>()

        var globalMax = 0
        val sizeX = Math.ceil(image.width / pixelsPerChunk.toDouble()).toInt()
        val sizeY = Math.ceil(image.height / pixelsPerChunk.toDouble()).toInt()
        val xRangeIter = 8..10
        val yRangeIter = 17..19

        center.set(-sizeX / 2, -sizeY / 2)

        xRangeIter.toList().parallelStream().map { x ->
            yRangeIter.mapNotNull { y ->

                val (chunk, localMax) = scan(x, y, xRange, yRange, data) ?: return@mapNotNull null
                globalMax = max(globalMax, localMax)

                (x + center.x to y + center.y) to chunk

            }
        }.forEach { it.forEach { map += it } }

        map.values.forEach { it.maxHeight = globalMax.toFloat() }

        info("Chunks in map: ${map.size}")
        return map
    }

    private fun scan(x: Int, y: Int, xRange: IntRange, yRange: IntRange, data: Raster): Pair<Chunk, Int>? {
        val scale = CHUNK_PIXELS * PIXEL_SIZE
        val posX = x * scale
        val posY = y * scale

//        val start = Vector3f((envelope.x - ORIGIN.x) + posX, 0f, (envelope.z - ORIGIN.z) + posY)
//
//        if (start.length() > 100000f) return null
//
//        val dist = (start.length() / 100000f)
//        val quality = -Math.log(dist + 0.1)
//        val vertexPerChunk = max(8, min(CHUNK_PIXELS, nearestPowerOf2((quality * CHUNK_PIXELS).toInt())))
        val vertexPerChunk = CHUNK_PIXELS

        val relScale = (CHUNK_PIXELS / vertexPerChunk)
        val heightMap = heightMapOfSize(vertexPerChunk + 1, vertexPerChunk + 1)
        var max = 0

        for (i in 0..vertexPerChunk) {
            for (j in 0..vertexPerChunk) {

                val absX = x * CHUNK_PIXELS + i * relScale
                val absY = y * CHUNK_PIXELS + j * relScale

                if (absX in xRange && absY in yRange) {
                    val pixel = data.getSample(absX, absY, 0)
                    if (pixel > 0) {
                        heightMap[i, j] = pixel.toFloat()
                        max = Math.max(max, pixel)
                    }
                }
            }
        }

        return Chunk(posX.toFloat(), posY.toFloat(), heightMap, 0f, scale.toFloat()) to max
    }

    fun bakeTerrain() {
        terrainLevel.toList().parallelStream().forEach { (pos, chunk) ->
            val key = "${pos.first},${pos.second},"
            val posKey = key + "p"
            val colorKey = key + "c"
            val geom = MeshBuilder.chunkToModel(chunk)

            geom.attributes.forEach {
                when (it.attributeName) {
                    "position" -> Rest.cacheMap[posKey] = it.data
                    "color" -> Rest.cacheMap[colorKey] = it.data
                }
            }
            chunk.cache = posKey to colorKey
        }
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
            info("Error loading level 0: $e")
            error = true
        }
        return error
    }
}