package com.cout970.server.terrain

import com.cout970.server.glTF.Vector2
import com.cout970.server.scene.DScene
import com.cout970.server.util.areaOf
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


                val height = when (xPixelPos in 0 until raster.width && yPixelPos in 0 until raster.height) {
                    true -> raster.getSample(xPixelPos, yPixelPos, 0)
                    else -> 0
                }

                map[x, y] = height.toFloat()
            }
        }

        return pos to Chunk(pos, map)
    }

    class TerrainView(
            val sector: TerrainLoader.TerrainSector,
            val offset: Vector2,
            val scale: Float
    )

    private fun createView(sector: TerrainLoader.TerrainSector, origin: Vector2): TerrainView {
        val x = origin.x - sector.origin.x
        val y = sector.origin.y - origin.y
        println("Terrain offset: $x, $y")
        return TerrainView(sector, Vector2(x, y), 1f / sector.pixelSize)
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

    private fun interpolate(a: Float, b: Float, c: Float): Float {
        // linear interpolation
        return a * (1 - c) + b * c

        // cosine interpolation
//        val mu2 = (1f - Math.cos(c * Math.PI).toFloat()) / 2f
//        return (a * (1f - mu2)) + (b * mu2)
    }

    fun load(scene: DScene): TerrainView {

        val terrain = TerrainLoader.loadTerrain(scene.ground.file)

        val area = scene.ground.area
        val chunkSize = 256
        val cellSize = terrain.pixelSize * chunkSize

        val startX = floor((area.pos.x - terrain.origin.x) / cellSize).toInt() - 1
        val startY = floor((terrain.origin.y - area.pos.y) / cellSize).toInt() - 1

        val sizeX = ceil(area.size.x / cellSize).toInt() + 2
        val sizeY = ceil(area.size.y / cellSize).toInt() + 2

        println("Loading terrain in x range $startX..${startX + sizeX} with chunksize of $chunkSize")
        println("Loading terrain in y range $startY..${startY + sizeY} with chunksize of $chunkSize")

        val sector = TerrainLoader.readSector(terrain, startX..(startX + sizeX), startY..(startY + sizeY), chunkSize)
        return createView(sector, scene.origin)
    }

    fun getHeight(view: TerrainLoader.TerrainView, x: Float, y: Float): Float = view.run {
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
}
