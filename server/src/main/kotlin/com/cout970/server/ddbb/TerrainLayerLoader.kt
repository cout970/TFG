package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Vector3
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.areaOf
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.Polyhedron
import org.joml.Vector3f

object TerrainLayerLoader : ILayerLoader {

    override fun load(area: Area): Layer {

        val terrain = generateMesh(160, 25)

        val terrainModel = Defs.Model(
                geometry = terrain,
                material = Defs.Material(
                        ambientIntensity = 0.0f,
                        shininess = 0f,
                        diffuseColor = Defs.Color(0.0f, 1.0f, 0.0f),
                        emissiveColor = Defs.Color(0f, 0f, 0f),
                        specularColor = Defs.Color(0f, 0f, 0f),
                        transparency = 0.0f
                )
        )

        val cubeShapePoint = Defs.Shape.ShapeAtPoint(
                model = terrainModel,
                position = Vector3f(0f, 0f, 0f),
                rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = Defs.GroundProjection.SnapProjection(0f)
        )

        return Layer(
                name = "Terrain height",
                description = "This layer shows the ground level",
                rules = listOf(Defs.Rule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = listOf(SceneBaker.bakeShape(cubeShapePoint))
                )),
                labels = emptyList()
        )
    }

    fun generateMesh(size: Int, scale: Int): Defs.Geometry {
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