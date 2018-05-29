package com.cout970.server.util

import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.rest.DPolygon
import com.cout970.server.rest.Polygon3d
import com.cout970.server.rest.Triangle2d
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models2d.Polygon as Polygon2d


object Triangulator {

    fun triangulate(polygon: Polygon2d): List<Triangle2d> {
        val points = polygon.getPoints().map { Vector2(it.x.toFloat(), it.y.toFloat()) }
        return triangulate(DPolygon(points))
    }

    fun triangulate(polygon: DPolygon): List<Triangle2d> {
        val points = polygon.points
        val data = points.flatMap { listOf(it.x, it.y) }.map { it.toDouble() }.toDoubleArray()

        val indices = Earcut.earcut(data)
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
        val plane = ab.cross(ac, Vector3()).normalize()
        val origin = points3d[0]

        // 3d axis representing the 2d axis in the plane space
        val orthoX = ab.normalize()
        val orthoY = orthoX.cross(plane, Vector3())

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

    private fun isClockwise(vertices: List<Coords3d>): Boolean {
        var sum = 0.0
        repeat(vertices.size) { i ->
            val v1 = vertices[i]
            val v2 = vertices[(i + 1) % vertices.size]
            sum += (v2.x - v1.x) * (v2.y + v1.y)
        }
        return sum > 0.0
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