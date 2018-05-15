package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Color
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Material
import com.cout970.server.rest.Defs.Model
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Rule
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Defs.Shape.ShapeAtSurface
import com.cout970.server.rest.Vector2
import com.cout970.server.rest.Vector3
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.relativize
import com.cout970.server.util.toGeometry
import com.cout970.server.util.toPolygon
import eu.printingin3d.javascad.coords.Dims3d
import eu.printingin3d.javascad.models.Cube
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point

object ParkLayerLoader : ILayerLoader {

    private val material = Material(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = Color(0f, 0.5f, 0f),
            emissiveColor = Color(0f, 0f, 0f),
            specularColor = Color(1f, 1f, 1f),
            transparency = 0f
    )

    val geometry = Cube(Dims3d(1.0, 10.0, 1.0)).toGeometry()

    override fun load(area: Area): Layer {
        val parks = loadFromDDBB(area).filter { it.areas.isNotEmpty() }
        val shapes = parks.map { b -> shapeOf(b) }

        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))

        return Layer(
                name = "Parks",
                description = "Shows parks of the city",
                rules = listOf(Rule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = bakedShapes
                )),
                labels = emptyList()
        )
    }

    private fun shapeOf(b: Park): Shape {
        return ShapeAtSurface(
                model = Model(geometry, material),
                surface = b.areas.first(),
                rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = Defs.GroundProjection.SnapProjection(0f),
                resolution = 0.01f
        )
    }

    private fun loadFromDDBB(area: Area): List<Park> {
        val sql = """
                SELECT geom, nombre, center
                FROM "parques (polígono)", $area AS area, ST_Centroid(geom) as center
                WHERE ST_Within(geom, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val name = it.getString("nombre")
            val centerGeom = it.getObject("center") as PGgeometry

            val center = centerGeom.geometry as Point
            val multiPolygon = geom.geometry as MultiPolygon

            Park(
                    areas = multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() },
                    name = name,
                    center = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
            )
        }
    }

    data class Park(val areas: List<Polygon>, val name: String, val center: Vector2)
}