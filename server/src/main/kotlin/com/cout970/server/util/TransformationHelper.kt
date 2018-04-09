package com.cout970.server.util

import com.cout970.server.rest.Defs
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.vrl.FacetGenerationContext

fun IModel.toGeometry(): Defs.Geometry {
    val coords = toCSG(FacetGenerationContext.DEFAULT)
            .polygons
            .flatMap { it.toFacets() }
            .map { it.triangle }
            .flatMap { it.points }
            .flatMap { listOf(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

    return MeshBuilder.buildGeometry(coords)
}