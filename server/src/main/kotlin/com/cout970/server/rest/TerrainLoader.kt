package com.cout970.server.rest

import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import java.awt.image.RenderedImage
import java.io.File

object TerrainLoader {

    fun loadLevel0Map() {
        val file = File("GaliciaDTM200m.tif")
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage

        rasterize(image, 64, 16) { chunk ->
            Rest.terrainLevel0 += chunk
        }
    }

    fun loadLevel1Map() {
        val file = File("GaliciaDTM25m.tif")
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage

        rasterize(image, 512, 16) { chunk ->
            Rest.terrainLevel1 += chunk
        }
    }

    fun rasterize(image: RenderedImage, chunkSize: Int, scale: Int, emit: (Pair<Pair<Int, Int>, HeightMap>) -> Unit) {
        val data = image.data
        val xRange = 0 until image.width
        val yRange = 0 until image.height
        val relScale = (chunkSize / scale)

        var max = 0
        for (x in 0..Math.ceil(image.width / chunkSize.toDouble()).toInt()) {
            for (y in 0..Math.ceil(image.height / chunkSize.toDouble()).toInt()) {

                val heightMap = heightMapOfSize(scale + 1, scale + 1)

                for (i in 0..scale) {
                    for (j in 0..scale) {

                        val absX = x * chunkSize + i * relScale
                        val absY = y * chunkSize + j * relScale

                        if (absX in xRange && absY in yRange) {
                            val pixel = data.getSample(absX, absY, 0)
                            if (pixel > 0) {
                                heightMap[i, j] = pixel.toFloat()
                                max = Math.max(max, pixel)
                            }
                        }
                    }
                }
                emit((x to y) to heightMap)
            }
        }
        println("Max: $max")
    }

    fun loadHeightMaps(): Boolean {
        var error = false
        try {
            loadLevel0Map()
        } catch (e: Exception) {
            println("Error loading level 0: $e")
            error = true
        }
        try {
            loadLevel1Map()
        } catch (e: Exception) {
            println("Error loading level 0: $e")
            error = true
        }
        return error
    }
}