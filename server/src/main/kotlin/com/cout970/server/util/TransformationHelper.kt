package com.cout970.server.util

import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.scene.DBufferGeometry
import com.cout970.server.scene.GeometryBuilder
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon
import com.cout970.server.scene.DPolygon as Polygon2D

@JvmName("toGeometryFromPolygon")
fun List<Polygon>.toGeometry(): DBufferGeometry {
    return flatMap { it.toFacets() }.map { it.triangle }.toGeometry()
}

@JvmName("toGeometryFromTriangles")
fun List<Triangle3d>.toGeometry(): DBufferGeometry {
    val coords = flatMap { it.points }.flatMap { listOf(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

    return GeometryBuilder.build(coords)
}

fun IModel.toGeometry(): DBufferGeometry {
    return toCSG(FacetGenerationContext.DEFAULT).polygons.toGeometry()
}

fun Polygon2D.triangles(): List<Vector2> {
    val triangles = Triangulator.triangulate(this)
    return triangles.flatMap { listOf(it.a, it.b, it.c) }
}

fun DBufferGeometry.max(): Vector3 {
    val data = attributes[0].data
    val max = Vector3()

    for (i in data.indices) {
        val mod = i % 3
        when (mod) {
            0 -> max.x = Math.max(max.x, data[i])
            1 -> max.y = Math.max(max.y, data[i])
            else -> max.z = Math.max(max.z, data[i])
        }
    }

    return max
}

fun DBufferGeometry.min(): Vector3 {
    val data = attributes[0].data
    val min = Vector3()

    for (i in data.indices) {
        val mod = i % 3
        when (mod) {
            0 -> min.x = Math.min(min.x, data[i])
            1 -> min.y = Math.min(min.y, data[i])
            else -> min.z = Math.min(min.z, data[i])
        }
    }

    return min
}

fun DBufferGeometry.center() = (max() - min()) * 0.5f + min()


private val field = run {
    Polygon::class.java.getDeclaredField("vertices").apply { isAccessible = true }
}

@Suppress("UNCHECKED_CAST")
val Polygon.vertices: List<Coords3d> get() = field.get(this) as List<Coords3d>