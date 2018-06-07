package com.cout970.server.util

import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.scene.DPolygon
import com.cout970.server.scene.Polygon3d
import com.cout970.server.scene.Triangle2d
import com.cout970.server.util.Earcut.earcut
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords2d.Coords2d
import org.joml.Quaternionf
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.PolygonPoint
import eu.printingin3d.javascad.models2d.Polygon as Polygon2d
import org.poly2tri.geometry.polygon.Polygon as PPolygon

object Triangulator {

    var USE_POLY2TRI = false

    fun triangulate(polygon: Polygon2d): List<Triangle2d> {
        val points = polygon.getPoints().map { Vector2(it.x.toFloat(), it.y.toFloat()) }
        return triangulate(DPolygon(points))
    }

    fun triangulate(polygon: DPolygon): List<Triangle2d> {

        if (USE_POLY2TRI) {
            try {
                val points = polygon.points.map { PolygonPoint(it.x.toDouble(), it.y.toDouble()) }
                val poly = PPolygon(points)

                polygon.holes.forEach {
                    poly.addHole(PPolygon(it.map { PolygonPoint(it.x.toDouble(), it.y.toDouble()) }))
                }

                Poly2Tri.triangulate(poly)

                return poly.triangles.map {
                    val p = it.points
                    Triangle2d(
                            Vector2(p[0].xf, p[0].yf),
                            Vector2(p[1].xf, p[1].yf),
                            Vector2(p[2].xf, p[2].yf)
                    )
                }
            } catch (e: Throwable) {
            }
        }

        val points = polygon.points
        val data = points.flatMap { listOf(it.x, it.y) }.map { it.toDouble() }.toDoubleArray()

        val indices = earcut(data)
        val triangles = mutableListOf<Triangle2d>()

        repeat(indices.size / 3) { i ->
            val a = points[indices[i * 3]]
            val b = points[indices[i * 3 + 1]]
            val c = points[indices[i * 3 + 2]]

            triangles += Triangle2d(Vector2(a), Vector2(b), Vector2(c))
        }

        return triangles
    }

    // this doesn't handle non-coplanar polygons
    fun triangulate(polyhedron: Polygon3d): List<Triangle3d> {
        val points3d = polyhedron.getPoints().map { Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }
        val projectionMap = mutableMapOf<Vector2, Vector3>()

        //https://stackoverflow.com/a/23474396

        // taking the first triangle as a plane
        val ab = points3d[1] - points3d[0]
        val ac = points3d[2] - points3d[0]

        // normal of the plane
        val normal = ab.cross(ac, Vector3()).normalize()
        val origin = points3d[0]

        if (USE_POLY2TRI) {
            try {
                val rot = Quaternionf()
                normal.rotationTo(Vector3(0f, 0f, 1f), rot)

                val points2d = points3d.map { rot.transform(it, Vector3()) }

                val points = points2d.map { PolygonPoint(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) }
                val poly = PPolygon(points)

                Poly2Tri.triangulate(poly)

                rot.invert()

                return poly.triangles.map {
                    val p = it.points

                    Triangle3d(
                            rot.transform(Vector3(p[0].xf, p[0].yf, p[0].zf), Vector3()).toCoords(),
                            rot.transform(Vector3(p[2].xf, p[2].yf, p[2].zf), Vector3()).toCoords(),
                            rot.transform(Vector3(p[1].xf, p[1].yf, p[1].zf), Vector3()).toCoords()
                    )
                }


            } catch (e: Throwable) {
            }
        }

        // 3d axis representing the 2d axis in the plane space
        val orthoX = ab.normalize()
        val orthoY = orthoX.cross(normal, Vector3())

        val points2d = points3d.map { point3d ->
            val relPoint = point3d - origin

            val x = orthoX.dot(relPoint)
            val y = orthoY.dot(relPoint)

            val point2d = Vector2(x, y)

            projectionMap += point2d to point3d

            point2d
        }

        // 2d triangulation
        val data = points2d.flatMap { listOf(it.x, it.y) }.map { it.toDouble() }.toDoubleArray()
        val indices = Earcut.earcut(data)
        val triangle3d = mutableListOf<Triangle3d>()

        repeat(indices.size / 3) { i ->
            // using the 2d indices to build 3d triangles
            triangle3d += Triangle3d(
                    points3d[indices[i * 3]].toCoords(),
                    points3d[indices[i * 3 + 1]].toCoords(),
                    points3d[indices[i * 3 + 2]].toCoords()
            )
        }

        return triangle3d
    }

    @Suppress("UNCHECKED_CAST")
    private fun Polygon3d.getPoints(): List<Coords3d> {
        val field = Polygon3d::class.java.getDeclaredField("vertices")
        field.isAccessible = true
        return field.get(this) as List<Coords3d>
    }

    @Suppress("UNCHECKED_CAST")
    private fun Polygon2d.getPoints(): List<Coords2d> {
        val field = Polygon2d::class.java.getDeclaredField("coords")
        field.isAccessible = true
        return field.get(this) as List<Coords2d>
    }
}