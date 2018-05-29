package com.cout970.server.ddbb

import com.cout970.server.rest.*
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.models.Cube
import org.joml.Vector3f
import org.postgis.PGgeometry
import org.postgis.Point

object LightsLayerLoader : ILayerLoader {

    private val material = DMaterial(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = DColor(1f, 1f, 0.0f),
            emissiveColor = DColor(0.1f, 0.1f, 0.1f),
            specularColor = DColor(.1f, 1f, 1f),
            transparency = 0f
    )

    private val geometry = Cube.fromCoordinates(
            Coords3d(0.4, 0.0, 0.4),
            Coords3d(0.6, 4.0, 0.6)
    ).addModel(Cube.fromCoordinates(
            Coords3d(0.0, 4.0, 0.0),
            Coords3d(1.0, 5.0, 1.0)
    )).toGeometry()

    override fun load(area: Area): DLayer {
        val shapes = loadFromDDBB(area).map { b -> shapeOf(b) }

        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))

        return DLayer(
                name = "Lights",
                description = "This layer shows all the light points",
                rules = listOf(DRule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = bakedShapes
                )),
                labels = emptyList()
        )
    }

    private fun shapeOf(b: Light): DShape {
        val (pos) = b
        return DShape.ShapeAtPoint(
                position = Vector3(pos.x, 0f, pos.y),
                rotation = DRotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = DGroundProjection.DefaultGroundProjection(0f, false),
                model = DModel(geometry, material)
        )
    }

    private fun loadFromDDBB(area: Area): List<Light> {
        val sql = """
                SELECT geom
                FROM "puntos de luz", $area AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """


        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val point = geom.geometry as Point

            Light(Vector2(
                    point.x.toFloat() - TerrainLoader.ORIGIN.x,
                    point.y.toFloat() - TerrainLoader.ORIGIN.z
            ))
        }
    }

    data class Light(val pos: Vector2)
}