package com.cout970.server.ddbb

object ParkLayerLoader : ILayerLoader {

//    private val material = DMaterial(
//            ambientIntensity = 0.5f,
//            shininess = 0f,
//            diffuseColor = DColor(0f, 0.5f, 0f),
//            emissiveColor = DColor(0f, 0f, 0f),
//            specularColor = DColor(1f, 1f, 1f),
//            transparency = 0f
//    )
//
//    val geometry = Cube(Dims3d(1.0, 10.0, 1.0)).toGeometry()
//
//    override fun load(area: Area): DLayer {
//        val parks = loadFromDDBB(area).filter { it.areas.isNotEmpty() }
//        val shapes = parks.map { b -> shapeOf(b) }
//
//        val bakedShapes = listOf(SceneBaker.bakeShapes(shapes))
//
//        return DLayer(
//                name = "Parks",
//                description = "Shows parks of the city",
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
//    private fun shapeOf(b: Park): DShape {
//        return ShapeAtSurface(
//                model = DModel(geometry, material),
//                surface = b.areas.first(),
//                rotation = DRotation(0f, Vector3f(0f, 0f, 0f)),
//                scale = Vector3(1f),
//                projection = DGroundProjection.SnapProjection(0f),
//                resolution = 0.01f
//        )
//    }
//
//    private fun loadFromDDBB(area: Area): List<Park> {
//        val sql = """
//                SELECT geom, nombre, center
//                FROM "parques (polígono)", $area AS area, ST_Centroid(geom) as center
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
//            Park(
//                    areas = multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() },
//                    name = name,
//                    center = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
//            )
//        }
//    }
//
//    data class Park(val areas: List<DPolygon>, val name: String, val center: Vector2)
}