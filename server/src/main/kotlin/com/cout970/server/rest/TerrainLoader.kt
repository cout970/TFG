package com.cout970.server.rest

import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.joml.Vector3f
import java.awt.image.RenderedImage
import java.io.File

object TerrainLoader {

    val ORIGIN = Vector3f(538973.6319697625f, 0f, 4750077.070013605f)
    val terrainLevel = mutableMapOf<Pair<Int, Int>, Chunk>()

    fun getHeight(x: Float, y: Float): Float {
        val scale = 512 * 25
        val pos = (x.toInt() / scale) to (y.toInt() / scale)
        val chunk: Chunk = terrainLevel[pos] ?: return 0f
        val gridX = (chunk.heights.width * (x - pos.first) / scale).toInt()
        val gridY = (chunk.heights.height * (y - pos.second) / scale).toInt()
        return chunk.heights[gridX, gridY]
    }

    fun loadLevel1Map() {
        val file = File("GaliciaDTM25m.tif")
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage
        val pos = coverage.envelope2D.minX.toFloat() to coverage.envelope2D.minY.toFloat()

        terrainLevel += rasterize(image, pos, 512, 16, 25f)
    }

    fun rasterize(image: RenderedImage, pos: Pair<Float, Float>, pixelsPerChunk: Int, vertexPerChunk: Int, meters: Float)
            : Map<Pair<Int, Int>, Chunk> {

        val data = image.data
        val xRange = 0 until image.width
        val yRange = 0 until image.height
        val relScale = (pixelsPerChunk / vertexPerChunk)

        val map = mutableMapOf<Pair<Int, Int>, Chunk>()

        var max = 0
        for (x in 0..Math.ceil(image.width / pixelsPerChunk.toDouble()).toInt()) {
            for (y in 0..Math.ceil(image.height / pixelsPerChunk.toDouble()).toInt()) {

                if (ORIGIN.distance(Vector3f(pos.first + x * pixelsPerChunk * meters, 0f, pos.second + y * pixelsPerChunk * meters)) < 100000) {

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
                    val posY = pos.second + y * pixelsPerChunk * meters - ORIGIN.y
                    map += (x to y) to Chunk(posX, posY, heightMap, 0f, meters * pixelsPerChunk)
                }
            }
        }
        map.values.forEach { it.maxHeight = max.toFloat() }

        println("Chunks in map: ${map.size}")
        return map
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