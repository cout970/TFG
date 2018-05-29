package com.cout970.server.ddbb

import com.cout970.server.rest.*
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.areaOf
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.Polyhedron
import org.joml.Vector3f

object TerrainLayerLoader : ILayerLoader {

    override fun load(area: Area): DLayer {

        val terrain = generateMesh(160, 25)

        val terrainModel = DModel(
                geometry = terrain,
                material = DMaterial(
                        ambientIntensity = 0.0f,
                        shininess = 0f,
                        diffuseColor = DColor(0.0f, 1.0f, 0.0f),
                        emissiveColor = DColor(0f, 0f, 0f),
                        specularColor = DColor(0f, 0f, 0f),
                        transparency = 0.0f
                )
        )

        val cubeShapePoint = DShape.ShapeAtPoint(
                model = terrainModel,
                position = Vector3f(0f, 0f, 0f),
                rotation = DRotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = DGroundProjection.SnapProjection(0f)
        )

        return DLayer(
                name = "Terrain height",
                description = "This layer shows the ground level",
                rules = listOf(DRule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = listOf(SceneBaker.bakeShape(cubeShapePoint))
                )),
                labels = emptyList()
        )
    }

    fun generateMesh(size: Int, scale: Int): DGeometry {
        val s = scale.toDouble()
        val i = (25f * size / scale).toInt()

        val triangles = areaOf(-i..i, -i..i).toList().flatMap { (x, y) ->
            listOf(
                    Triangle3d(Coords3d((x + 1) * s, 0.0, (y + 1) * s), Coords3d((x + 1) * s, 0.0, y * s), Coords3d(x * s, 0.0, y * s)),
                    Triangle3d(Coords3d(x * s, 0.0, (y + 1) * s), Coords3d((x + 1) * s, 0.0, (y + 1) * s), Coords3d(x * s, 0.0, y * s))
            )
        }

        return Polyhedron(triangles).toGeometry()
    }
}