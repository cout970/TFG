package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Vector3
import com.cout970.server.util.*
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry

object StreetLayerLoader : ILayerLoader {

    private const val diffuseColor = 63.1f / 360f

    private val material = Defs.Material(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = colorFromHue(diffuseColor),
            emissiveColor = Defs.Color(0f, 0f, 0f),
            specularColor = Defs.Color(1f, 1f, 1f),
            transparency = 0f
    )

    override fun load(area: Area): Defs.Layer {
        val shapes = loadFromDDBB(area).filter { it.areas.isNotEmpty() }.map { b -> shapeOf(b) }

        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))

        return Defs.Layer(
                name = "Streets",
                description = "This layer shows the streets of the city",
                rules = listOf(Defs.Rule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = bakedShapes
                )),
                labels = emptyList()
        )
    }

    private fun shapeOf(b: Street): Defs.Shape {
        return Defs.Shape.ShapeAtPoint(
                Defs.Model(b.areas.toGeometry(), material),
                rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = Defs.GroundProjection.SnapProjection(1f),
                position = Vector3f()
        )
    }

    private fun loadFromDDBB(area: Area): List<Street> {
        val sql = """
                SELECT geom
                FROM calles, $area AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry

            val multiPolygon = geom.geometry as MultiPolygon

            Street(multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() })
        }
    }

    data class Street(val areas: List<Defs.Polygon>)
}