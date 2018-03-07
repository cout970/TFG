package com.cout970.server.ddbb

import com.cout970.server.rest.Model
import com.cout970.server.rest.Shape
import com.cout970.server.rest.ShapeType
import com.cout970.server.util.Earcut
import eu.printingin3d.javascad.basic.Angle
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models.LinearExtrude
import org.joml.Vector3f
import org.postgis.*


object ShapeDAO {

    fun getMultiLine(mun: String): Model {

        require("""\d\d\d""".toRegex().matches(mun)) { "Invalid municipio code: $mun" }
        val lines = mutableListOf<List<Vector3f>>()

        DDBBManager.useConnection {

            query("""SELECT geom, cota FROM testing.curvas WHERE municipio = '$mun' LIMIT 1000;""").forEach {
                val cota = it.getDouble("cota")
                val geom = it.getObject("geom") as PGgeometry
                val multiLine = geom.geometry as MultiLineString

                val line = mutableListOf<Vector3f>()

                multiLine.lines.forEach {
                    it.points.forEach {
                        it.z = cota
                        addPoint(it, line)
                    }
                }

                lines.add(line)
            }
        }

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        lines.forEach {
            val start = vertices.size
            shapes.add(Shape((0 until it.size).map { it + start }))
            vertices.addAll(it)
        }

        return Model(vertices, shapes, ShapeType.LINE)
    }

    fun getBuildings(pos: Pair<Int, Int>): Model {

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {

            val minX = 538973.6319697625f + pos.first * 1000
            val minY = 4750077.070013605f + pos.second * 1000

            val maxX = 538973.6319697625f + pos.first * 1000 + 1000
            val maxY = 4750077.070013605f + pos.second * 1000 + 1000

            val area = "$minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY"
            val sql = """
                SELECT geom, plantas
                FROM "edificación alturas", ST_GeomFromText('POLYGON(($area))') AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

            var count = 0

            query(sql).onEach { count++ }.forEach {
                val geom = it.getObject("geom") as PGgeometry
                val foors = it.getInt("plantas")
                val multiPolygon = geom.geometry as MultiPolygon

                multiPolygon.polygons.forEach loop@{ polygon ->
                    addPolygon(polygon, foors, vertices, shapes)
                }
            }
            println("Loaded $count geometries")
            System.gc()
        }

        return Model(vertices, shapes, ShapeType.MESH)
    }


    private fun addPolygon(polygon: Polygon, foors: Int, vertices: MutableList<Vector3f>, shapes: MutableList<Shape>) {
        val points = polygon.getRing(0).points
        val p = eu.printingin3d.javascad.models2d.Polygon(points.map { Coords2d(it.x, it.y) })

        val csg = LinearExtrude(p, foors * 3.5, Angle.ZERO).toCSG()
        val rings = csg.polygons.map { it.getVertices() }

        rings.forEach { ring ->

            if (ring.size > 3) {
                val vertex = DoubleArray(ring.size * 2)
                var index = 0
                val offset = vertices.size

                ring.forEach {
                    vertex[index++] = it.x
                    vertex[index++] = it.y
                    addPoint(Point(it.x, it.y, it.z), vertices)
                }

                val indices = Earcut.earcut(vertex)

                for (j in 0 until indices.size / 3) {
                    shapes += Shape(listOf(
                            offset + indices[j * 3],
                            offset + indices[j * 3 + 1],
                            offset + indices[j * 3 + 2]
                    ))
                }
            } else {
                val offset = vertices.size
                ring.forEach {
                    addPoint(Point(it.x, it.y, it.z), vertices)
                }
                shapes += Shape(listOf(
                        offset,
                        offset + 1,
                        offset + 2
                ))
            }
        }
    }

    fun eu.printingin3d.javascad.vrl.Polygon.getVertices(): List<Coords3d> {
        val field = eu.printingin3d.javascad.vrl.Polygon::class.java.getDeclaredField("vertices")
        field.isAccessible = true
        return field.get(this) as List<Coords3d>
    }

//    private fun addPolygon(polygon: Polygon, foors: Int, vertices: MutableList<Vector3f>, shapes: MutableList<Shape>) {
//
//        for (i in 0 until polygon.numRings()) {
//            val ring = polygon.getRing(i)
//
//            val vertex = DoubleArray(ring.numPoints() * 2)
//            var index = 0
//            val offset = vertices.size
//
//            ring.points.forEach {
//                vertex[index++] = it.x
//                vertex[index++] = it.y
//                addPoint(it, vertices)
//            }
//
//            val indices = Earcut.earcut(vertex)
//
//            for (j in 0 until indices.size / 3) {
//                shapes += Shape(listOf(
//                        offset + indices[j * 3],
//                        offset + indices[j * 3 + 1],
//                        offset + indices[j * 3 + 2]
//                ))
//            }
//        }
//    }

    private fun addPoint(p: Point, vertexList: MutableList<Vector3f>) {
        val minX = 538973.6319697625f
        val minY = 4750077.070013605f

        val scale = 1
        val hScale = 1

        vertexList.add(Vector3f(
                scale * -(p.x.toFloat() - minX),
                hScale * (p.z.toFloat()),
                scale * (p.y.toFloat() - minY)
        ))
    }
}