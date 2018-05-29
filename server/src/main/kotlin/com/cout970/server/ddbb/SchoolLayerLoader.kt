package com.cout970.server.ddbb

object SchoolLayerLoader : ILayerLoader {

//    private const val diffuseColor = 240.1f / 360f
//
//    private val material = DMaterial(
//            ambientIntensity = 0.5f,
//            shininess = 0f,
//            diffuseColor = colorFromHue(diffuseColor),
//            emissiveColor = DColor(0f, 0f, 0f),
//            specularColor = DColor(1f, 1f, 1f),
//            transparency = 0.25f
//    )
//
//    override fun load(area: Area): DLayer {
//        val schools = loadFromDDBB(area).filter { it.areas.isNotEmpty() }
//        val shapes = schools.map { b -> shapeOf(b) }
//        val labels = schools.map { labelOf(it) }
//
//        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))
//
//        return DLayer(
//                name = "Schools and colleges",
//                description = "Marks the are specified as schools",
//                rules = listOf(DRule(
//                        filter = "ignore",
//                        minDistance = 0f,
//                        maxDistance = 2000f,
//                        shapes = bakedShapes
//                )),
//                labels = labels
//        )
//    }
//
//    private fun shapeOf(b: School): DShape {
//        return DShape.ExtrudeSurface(
//                surface = b.areas.first(),
//                height = 1f,
//                rotation = DRotation(0f, Vector3f(0f, 0f, 0f)),
//                scale = Vector3(1f),
//                projection = DGroundProjection.DefaultGroundProjection(0f, false),
//                material = material
//        )
//    }
//
//    private fun labelOf(p: School): DLabel {
//
//        val xPos = p.center.x + TerrainLoader.ORIGIN.x
//        val zPos = p.center.y + TerrainLoader.ORIGIN.z
//
//        val height = TerrainLoader.getHeight(xPos, zPos) + 25f
//
//        return DLabel(
//                txt = p.name,
//                position = Vector3(p.center.x, height, p.center.y),
//                scale = 5.0
//        )
//    }
//
//    private fun loadFromDDBB(area: Area): List<School> {
//        val sql = """
//                SELECT geom, nombre, center
//                FROM "centros de enseñanza (polígono)", ST_Centroid(geom) as center, $area AS area
//                WHERE ST_Within(geom, area);
//                      """
//
//        return DDBBManager.load(sql) {
//
//            val geom = it.getObject("geom") as PGgeometry
//            val name = it.getString("nombre")
//            val centerGeom = it.getObject("center") as PGgeometry
//
//            val center = centerGeom.geometry as Point
//            val multiPolygon = geom.geometry as MultiPolygon
//
//            School(
//                    areas = multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() },
//                    name = name,
//                    center = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
//            )
//        }
//    }
//
//    data class School(val areas: List<DPolygon>, val name: String, val center: Vector2)
}