package com.cout970.server.terrain

import com.cout970.server.rest.Chunk
import com.cout970.server.rest.HeightMap
import com.cout970.server.rest.Vector2
import com.cout970.server.rest.heightMapOfSize
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
import kotlin.streams.toList

object TerrainLoader {

    //    val ORIGIN = Vector3f(475456.0f, 0f, 4849634.0f) map origin
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

    fun createView(sector: TerrainSector, origin: Vector2, unitSize: Float): TerrainView {
        val x = origin.x - sector.origin.x
        val y = sector.origin.y - origin.y
        println("offset: $x, $y")
        return TerrainView(sector, Vector2(x, y), 1f / unitSize)
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

    private fun HeightMap.interpolatedHeight(x: Float, y: Float): Float {
        val gridX = x.toInt()
        val gridY = y.toInt()

        return this[gridX, gridY]
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
        val absX = x // - tmpTerrainView.relativeOrigin.x
        val absY = y // - tmpTerrainView.relativeOrigin.y

        return tmpTerrainView.getHeight(absX, absY)
    }

    private fun interpolate(a: Float, b: Float, c: Float): Float {
        return a * c + b * (1 - c)
//        val mu2 = (1f - Math.cos(c * Math.PI).toFloat()) / 2f
//        return (a * (1f - mu2)) + (b * mu2)
    }

    fun loadHeightMaps(): Boolean {
        var error = false
        try {
            val terrain = loadTerrain("../data/GaliciaDTM25m.tif")
            val sector = readSector(terrain, 0..31, 0..31, 256)
            tmpTerrainView = createView(sector, Vector2(ORIGIN.x, ORIGIN.z), 25f)
        } catch (e: Exception) {
            info("Error loading terrain: $e")
            error = true
        }
        return error
    }
}