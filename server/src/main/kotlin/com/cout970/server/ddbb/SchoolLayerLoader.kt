package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Label
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Vector2
import com.cout970.server.rest.Vector3
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.colorFromHue
import com.cout970.server.util.relativize
import com.cout970.server.util.toPolygon
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point

object SchoolLayerLoader : ILayerLoader {

    private const val diffuseColor = 240.1f / 360f

    private val material = Defs.Material(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = colorFromHue(diffuseColor),
            emissiveColor = Defs.Color(0f, 0f, 0f),
            specularColor = Defs.Color(1f, 1f, 1f),
            transparency = 0.25f
    )

    override fun load(area: Area): Layer {
        val schools = loadFromDDBB(area).filter { it.areas.isNotEmpty() }
        val shapes = schools.map { b -> shapeOf(b) }
        val labels = schools.map { labelOf(it) }

        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))

        return Layer(
                name = "Schools and colleges",
                description = "Marks the are specified as schools",
                rules = listOf(Defs.Rule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = bakedShapes
                )),
                labels = labels
        )
    }

    private fun shapeOf(b: School): Shape {
        return Shape.ExtrudeSurface(
                surface = b.areas.first(),
                height = 1f,
                rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = Defs.GroundProjection.DefaultGroundProjection(0f, false),
                material = material
        )
    }

    private fun labelOf(p: School): Label {

        val xPos = p.center.x + TerrainLoader.ORIGIN.x
        val zPos = p.center.y + TerrainLoader.ORIGIN.z

        val height = TerrainLoader.getHeight(xPos, zPos) + 25f

        return Label(
                txt = p.name,
                position = Vector3(p.center.x, height, p.center.y),
                scale = 5.0
        )
    }

    private fun loadFromDDBB(area: Area): List<School> {
        val sql = """
                SELECT geom, nombre, center
                FROM "centros de enseñanza (polígono)", ST_Centroid(geom) as center, $area AS area
                WHERE ST_Within(geom, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val name = it.getString("nombre")
            val centerGeom = it.getObject("center") as PGgeometry

            val center = centerGeom.geometry as Point
            val multiPolygon = geom.geometry as MultiPolygon

            School(
                    areas = multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() },
                    name = name,
                    center = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
            )
        }
    }

    data class School(val areas: List<Polygon>, val name: String, val center: Vector2)
}