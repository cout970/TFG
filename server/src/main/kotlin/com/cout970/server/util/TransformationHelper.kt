package com.cout970.server.util

import com.cout970.server.rest.Defs
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon

@JvmName("toGeometryFromPolygon")
fun List<Polygon>.toGeometry(): Defs.Geometry {
    return flatMap { it.toFacets() }.map { it.triangle }.toGeometry()
}

@JvmName("toGeometryFromTriangles")
fun List<Triangle3d>.toGeometry(): Defs.Geometry {
    val coords = flatMap { it.points }.flatMap { listOf(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

    return MeshBuilder.buildGeometry(coords)
}

fun IModel.toGeometry(): Defs.Geometry {
    return toCSG(FacetGenerationContext.DEFAULT).polygons.toGeometry()
}