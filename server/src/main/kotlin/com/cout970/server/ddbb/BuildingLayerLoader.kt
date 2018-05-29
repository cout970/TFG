package com.cout970.server.ddbb

object BuildingLayerLoader : ILayerLoader {

//    private const val diffuseColor = 273.1f / 360f
//
//    private val material = DMaterial(
//            ambientIntensity = 0.5f,
//            shininess = 0f,
//            diffuseColor = colorFromHue(diffuseColor),
//            emissiveColor = DColor(0f, 0f, 0f),
//            specularColor = DColor(1f, 1f, 1f),
//            transparency = 0f
//    )
//
//    override fun load(area: Area): DLayer {
//        val shapes = loadFromDDBB(area).filter { it.areas.isNotEmpty() }.map { b -> shapeOf(b) }
//
//        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))
//
//        return DLayer(
//                name = "Buildings",
//                description = "This layer shows some buildings",
//                rules = listOf(DRule(
//                        filter = "ignore",
//                        minDistance = 0f,
//                        maxDistance = 2000f,
//                        shapes = bakedShapes
//                )),
//                labels = emptyList()
//        )
//    }
//
//    private fun shapeOf(b: Building): DShape {
//        return DShape.ExtrudeSurface(
//                surface = b.areas.first(),
//                height = b.floors * 3.5f,
//                rotation = DRotation(0f, Vector3f(0f, 0f, 0f)),
//                scale = Vector3(1f),
//                projection = DGroundProjection.DefaultGroundProjection(0f, false),
//                material = material
//        )
//    }
//
//    private fun loadFromDDBB(area: Area): List<Building> {
//        val sql = """
//                SELECT geom, plantas
//                FROM "edificación alturas", $area AS area
//                WHERE ST_Within(geom, area);
//                      """
//
//        return DDBBManager.load(sql) {
//
//            val geom = it.getObject("geom") as PGgeometry
//            val floors = it.getInt("plantas")
//
//            val multiPolygon = geom.geometry as MultiPolygon
//
//            Building(multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() }, floors)
//        }
//    }
//
//    data class Building(val areas: List<DPolygon>, val floors: Int)
}