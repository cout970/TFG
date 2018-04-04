package com.cout970.server.ddbb

import com.cout970.server.rest.Model
import com.cout970.server.rest.Shape
import com.cout970.server.rest.ShapeType
import com.cout970.server.rest.TerrainLoader
import com.cout970.server.util.Earcut
import com.cout970.server.util.earthToScene
import eu.printingin3d.javascad.basic.Angle
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models.LinearExtrude
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point
import org.postgis.Polygon


object ShapeDAO {


    private fun getAreaString(pos: Pair<Int, Int>): String {
        val minX = TerrainLoader.ORIGIN.x + pos.first * 1000
        val minY = TerrainLoader.ORIGIN.z + pos.second * 1000
        val maxX = TerrainLoader.ORIGIN.x + pos.first * 1000 + 1000
        val maxY = TerrainLoader.ORIGIN.z + pos.second * 1000 + 1000

        return "$minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY"
    }

    fun getStreets(pos: Pair<Int, Int>): Model {

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {

            val sql = """
                SELECT geom, ancho
                FROM calles, ST_GeomFromText('POLYGON((${getAreaString(pos)}))') AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

            var count = 0

            query(sql).onEach { count++ }.forEach {
                val geom = it.getObject("geom") as PGgeometry
                val ancho = it.getInt("ancho")
                val geometry = geom.geometry

                val multiPolygon = geom.geometry as MultiPolygon

                multiPolygon.polygons.forEach loop@ { polygon ->
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

    fun getBuildings(pos: Pair<Int, Int>): Model {

        val vertices = mutableListOf<Vector3f>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {

            val sql = """
                SELECT geom, plantas
                FROM "edificación alturas", ST_GeomFromText('POLYGON((${getAreaString(pos)}))') AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

            var count = 0

            query(sql).onEach { count++ }.forEach {
                val geom = it.getObject("geom") as PGgeometry
                val floors = it.getInt("plantas")
                val multiPolygon = geom.geometry as MultiPolygon

                multiPolygon.polygons.forEach loop@ { polygon ->
                    (0 until polygon.numRings())
                            .map { polygon.getRing(it) }
                            .forEach { ring ->
                                val points = ring.points
                                        .map { it.toVextor3f().apply { y = floors * 3.5f } }

                                shapes.add(Shape((vertices.size until vertices.size + points.size).toList()))
                                vertices.addAll(points)
                            }
//                    addPolygon(polygon, floors, vertices, shapes)
                }
            }
            println("Loaded $count geometries")
            System.gc()
        }

        return Model(vertices, shapes, ShapeType.POLYGONS)
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
                    vertices += Point(it.x, it.y, it.z).toVextor3f()
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
                    vertices += Point(it.x, it.y, it.z).toVextor3f()
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

    fun Point.toVextor3f(): Vector3f {
        val scale = 1
        val hScale = 1

        val (i, j) = earthToScene(x.toFloat() to y.toFloat())

        return Vector3f(
                scale * i,
                hScale * z.toFloat(),
                scale * j
        )
    }
}