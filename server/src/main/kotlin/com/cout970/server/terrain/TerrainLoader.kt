package com.cout970.server.terrain

import com.cout970.server.glTF.Vector2
import com.cout970.server.util.areaOf
import com.cout970.server.util.info
import com.cout970.server.util.upTo
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.gce.geotiff.GeoTiffReader
import org.joml.Vector2i
import org.joml.Vector3f
import java.awt.Point
import java.awt.image.Raster
import java.awt.image.RenderedImage
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.streams.toList

object TerrainLoader {

    val ORIGIN = Vector3f(535909f, 0f, 4746842f)

    lateinit var tmpTerrainView: TerrainView

    class TerrainFile(
            val origin: Vector2,
            val image: RenderedImage,
            val pixelSize: Float
    )

    private fun loadTerrain(path: String): TerrainFile {
        val file = File(path)
        val reader = GeoTiffReader(file)
        val coverage = reader.read(null) as GridCoverage2D
        val image = coverage.renderedImage
        val pixelSize = (coverage.envelope2D.maxX - coverage.envelope2D.minX).toFloat() / image.width
        val origin = coverage.gridGeometry.gridToCRS2D.transform(Point(0, 0), Point())

        return TerrainFile(
                Vector2(origin.x.toFloat(), origin.y.toFloat()),
                image,
                pixelSize
        )
    }

    class TerrainSector(
            val chunkMap: Map<Vector2i, Chunk>,
            val origin: Vector2,
            val chunkSize: Int,
            val pixelSize: Float
    )

    private fun readSector(terrain: TerrainFile, rangeX: IntRange, rangeY: IntRange, chunkSize: Int): TerrainSector {
        val chunkCoords = areaOf(rangeX, rangeY).toList()
        val data = terrain.image.data

        val chunks = chunkCoords.parallelStream().map { (x, y) -> readChunk(Vector2i(x, y), data, chunkSize) }.toList().toMap()

        return TerrainSector(chunks, terrain.origin, chunkSize, terrain.pixelSize)
    }

    private fun readChunk(pos: Vector2i, raster: Raster, chunkSize: Int): Pair<Vector2i, Chunk> {

        val map = heightMapOfSize(chunkSize, chunkSize)

        val xStart = pos.x * chunkSize
        val yStart = pos.y * chunkSize

        for (x in 0 upTo chunkSize) {
            for (y in 0 upTo chunkSize) {

                val xPixelPos = x + xStart
                val yPixelPos = y + yStart
                val height = raster.getSample(xPixelPos, yPixelPos, 0)

                map[x, y] = height.toFloat()
            }
        }

        return pos to Chunk(pos, map)
    }

    class TerrainView(
            val sector: TerrainSector,
            val offset: Vector2,
            val scale: Float
    )

    fun createView(sector: TerrainSector, origin: Vector2): TerrainView {
        val x = origin.x - sector.origin.x
        val y = sector.origin.y - origin.y
        println("offset: $x, $y")
        return TerrainView(sector, Vector2(x, y), 1f / sector.pixelSize)
    }

    fun TerrainView.getHeight(x: Float, y: Float): Float {
        val relativeX = (offset.x + x) * scale
        val relativeY = (offset.y + y) * scale

        if (relativeX < 0 || relativeY < 0) return -100f

        val chunkPos = Vector2i(
                (relativeX / sector.chunkSize).toInt(),
                (relativeY / sector.chunkSize).toInt()
        )

        val chunk = sector.chunkMap[chunkPos] ?: return 0f

        val chunkRelX = relativeX - chunkPos.x * sector.chunkSize
        val chunkRelY = relativeY - chunkPos.y * sector.chunkSize

        return chunk.heights.interpolatedHeight(chunkRelX, chunkRelY)
    }

    private fun HeightMap.uninterpolateHeight(x: Float, y: Float): Float {
        return this[x.toInt(), y.toInt()]
    }

    private fun HeightMap.interpolatedHeight(x: Float, y: Float): Float {
        val gridX = floor(x)
        val gridY = floor(y)

        val gridX1 = ceil(x)
        val gridY1 = ceil(y)

        val base = this[x.toInt(), y.toInt()]

        val minXminY = this.getOrNull(gridX.toInt(), gridY.toInt()) ?: base
        val maxXminY = this.getOrNull(gridX1.toInt(), gridY.toInt()) ?: base

        val minXmaxY = this.getOrNull(gridX.toInt(), gridY1.toInt()) ?: base
        val maxXmaxY = this.getOrNull(gridX1.toInt(), gridY1.toInt()) ?: base

        val deltaX = (x - gridX).coerceIn(0.0f, 1.0f)
        val deltaY = (y - gridY).coerceIn(0.0f, 1.0f)

        // Bilinear interpolation
        return interpolate(
                interpolate(minXminY, maxXminY, deltaX),
                interpolate(minXmaxY, maxXmaxY, deltaX),
                deltaY
        )
    }

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
        return tmpTerrainView.getHeight(x, y)
    }

    private fun interpolate(a: Float, b: Float, c: Float): Float {
        // linear interpolation
        return a * (1 - c) + b * c

        // cosine interpolation
//        val mu2 = (1f - Math.cos(c * Math.PI).toFloat()) / 2f
//        return (a * (1f - mu2)) + (b * mu2)
    }

    fun loadHeightMaps(): Boolean {
        var error = false
        try {
            val terrain = loadTerrain("../data/GaliciaDTM25m.tif")
            val sector = readSector(terrain, 8..11, 14..17, 256)
            tmpTerrainView = createView(sector, Vector2(ORIGIN.x, ORIGIN.z))
        } catch (e: Exception) {
            info("Error loading terrain: $e")
            error = true
        }
        return error
    }
}