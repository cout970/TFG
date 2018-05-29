package com.cout970.server.util

import com.cout970.server.rest.DGeometry
import com.cout970.server.rest.Vector2
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon
import com.cout970.server.rest.DPolygon as Polygon2D

@JvmName("toGeometryFromPolygon")
fun List<Polygon>.toGeometry(): DGeometry {
    return flatMap { it.toFacets() }.map { it.triangle }.toGeometry()
}

@JvmName("toGeometryFromTriangles")
fun List<Triangle3d>.toGeometry(): DGeometry {
    val coords = flatMap { it.points }.flatMap { listOf(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

    return MeshBuilder.buildGeometry(coords)
}

fun IModel.toGeometry(): DGeometry {
    return toCSG(FacetGenerationContext.DEFAULT).polygons.toGeometry()
}

fun Polygon2D.triangles(): List<Vector2> {
    val data = DoubleArray(points.size * 2)

    points.forEachIndexed { index, point ->
        data[index * 2] = point.x.toDouble()
        data[index * 2 + 1] = point.y.toDouble()
    }

    val indices = Earcut.earcut(data)

    val list = mutableListOf<Vector2>()

    indices.windowed(3,3).forEach {
        list += points[it[2]]
        list += points[it[1]]
        list += points[it[0]]
    }

    return list
}