package com.cout970.server.ddbb

import com.cout970.server.rest.*
import com.cout970.server.util.MeshBuilder
import com.cout970.server.util.TerrainLoader
import com.cout970.server.util.earthToScene
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point
import javax.xml.parsers.DocumentBuilderFactory


object ShapeDAO {

    private fun getAreaString(pos: Pair<Int, Int>): String {
        val minX = TerrainLoader.ORIGIN.x + pos.first * 1000
        val minY = TerrainLoader.ORIGIN.z + pos.second * 1000
        val maxX = TerrainLoader.ORIGIN.x + pos.first * 1000 + 1000
        val maxY = TerrainLoader.ORIGIN.z + pos.second * 1000 + 1000

        return "ST_GeomFromText('POLYGON(($minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY))')"
    }

    fun getStreets(pos: Pair<Int, Int>): Model {

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {

            val sql = """
                SELECT geom, ancho
                FROM calles, ${getAreaString(pos)} AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

            var count = 0

            query(sql).onEach { count++ }.forEach {
                val geom = it.getObject("geom") as PGgeometry
                val ancho = it.getInt("ancho")
                val geometry = geom.geometry

                val multiPolygon = geom.geometry as MultiPolygon

                multiPolygon.polygons.forEach loop@{ polygon ->
                    (0 until polygon.numRings())
                            .map { polygon.getRing(it) }
                            .forEach { ring ->
                                val points = ring.points.map { it.toVextor3f() }

                                shapes.add(Shape((vertices.size until vertices.size + points.size).toList()))
                                vertices.addAll(points)
                            }
                }
            }
            System.gc()
        }

        return Model(vertices, shapes, ShapeType.POLYGONS)
    }

    fun getBuildingsIn(pos: Pair<Int, Int>): Defs.Geometry {
        val models: MutableList<Defs.Geometry> = mutableListOf()
        var size = 0

        DDBBManager.useConnection {

            val query1 = """
                SELECT ST_AsX3D(result) as x3d
                FROM (SELECT geom
                      FROM "edificación alturas", ${getAreaString(pos)} AS area
                      WHERE ST_Within(geom, area)) as building,
                     st_tesselate(building.geom) as triangleMesh,
                     st_extrude(triangleMesh, 0, 0, 0) as extruded,
                     st_geometryn(extruded, 1) as result;
                """

            query(query1).forEach {
                val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val document = documentBuilder.parse(it.getAsciiStream("x3d"))

                val indexedFaceSet = document.childNodes.item(0)
                val coordinates = indexedFaceSet.childNodes.item(0)

                val indicesStr = indexedFaceSet.attributes.getNamedItem("coordIndex").nodeValue
                val posStr = coordinates.attributes.getNamedItem("point").nodeValue

                val indices = indicesStr.split(' ').filter { it != "-1" }.map { it.toInt() }
                val coords = posStr.split(' ').map { it.toDouble() }

                models += MeshBuilder.buildGeometry(indices, TerrainLoader.relativize(coords))
                size++
            }
        }

        val start = System.currentTimeMillis()

        val result = models.fold(Defs.Geometry(emptyList())) { a, b -> a.merge(b) }

        println("All models ($size) merged in ${System.currentTimeMillis() - start} ms")
        return result
    }

    fun getBuildings(pos: Pair<Int, Int>): Model {

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {

            val sql = """
                SELECT geom, plantas
                FROM "edificación alturas", ${getAreaString(pos)} AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

            var count = 0

            query(sql).onEach { count++ }.forEach {
                val geom = it.getObject("geom") as PGgeometry
                val floors = it.getInt("plantas")
                val multiPolygon = geom.geometry as MultiPolygon

                multiPolygon.polygons.forEach loop@{ polygon ->
                    (0 until polygon.numRings())
                            .map { polygon.getRing(it) }
                            .forEach { ring ->
                                val points = ring.points
                                        .map {
                                            val h = TerrainLoader.getHeight(it.x.toFloat(), it.y.toFloat())
                                            it.toVextor3f().apply { y = h + floors * 3.5f }
                                        }

                                shapes.add(Shape((vertices.size until vertices.size + points.size).toList()))
                                vertices.addAll(points)
                            }
                }
            }
            println("Loaded $count geometries")
            System.gc()
        }

        return Model(vertices, shapes, ShapeType.POLYGONS)
    }

    fun Defs.Geometry.merge(other: Defs.Geometry): Defs.Geometry {
        val attrMap = mutableMapOf<String, Defs.BufferAttribute>()

        attributes.forEach {
            attrMap[it.attributeName] = it
        }

        other.attributes.forEach {
            if (it.attributeName in attrMap) {
                attrMap[it.attributeName] = it.merge(attrMap.getValue(it.attributeName))
            } else {
                attrMap[it.attributeName] = it
            }
        }

        return Defs.Geometry(attrMap.values.toList())
    }

    fun Defs.BufferAttribute.merge(other: Defs.BufferAttribute): Defs.BufferAttribute {
        return Defs.BufferAttribute(attributeName, data + other.data, count)
    }

    fun Point.toVextor3f(): Vector3f {
        val scale = 1
        val hScale = 1

        val (i, j) = earthToScene(x.toFloat() to y.toFloat())

        return Vector3f(
                scale * -i,
                hScale * z.toFloat(),
                scale * j
        )
    }
}
