package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Vector3
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.colorFromHue
import com.cout970.server.util.relativize
import com.cout970.server.util.toPolygon
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry

object BuildingLayerLoader : ILayerLoader {

    private const val diffuseColor = 273.1f / 360f

    private val material = Defs.Material(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = colorFromHue(diffuseColor),
            emissiveColor = Defs.Color(0f, 0f, 0f),
            specularColor = Defs.Color(1f, 1f, 1f),
            transparency = 0f
    )

    override fun load(area: Area): Layer {
        val shapes = loadFromDDBB(area).filter { it.areas.isNotEmpty() }.map { b -> shapeOf(b) }

        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))

        return Layer(
                name = "Buildings",
                description = "This layer shows some buildings",
                rules = listOf(Defs.Rule(
                        filter = "ignore",
                        minDistance = 0f,
                        maxDistance = 2000f,
                        shapes = bakedShapes
                )),
                labels = emptyList()
        )
    }

    private fun shapeOf(b: Building): Shape {
        return Shape.ExtrudeSurface(
                surface = b.areas.first(),
                height = b.floors * 3.5f,
                rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                scale = Vector3(1f),
                projection = Defs.GroundProjection.DefaultGroundProjection(0f, false),
                material = material
        )
    }

    private fun loadFromDDBB(area: Area): List<Building> {
        val sql = """
                SELECT geom, plantas
                FROM "edificación alturas", $area AS area
                WHERE ST_Within(geom, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val floors = it.getInt("plantas")

            val multiPolygon = geom.geometry as MultiPolygon

            Building(multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() }, floors)
        }
    }

    data class Building(val areas: List<Polygon>, val floors: Int)
}