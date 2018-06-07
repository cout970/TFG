package com.cout970.server.util

import com.cout970.server.glTF.Vector2
import com.cout970.server.scene.*
import com.cout970.server.terrain.TerrainLoader
import eu.printingin3d.javascad.coords.Coords3d

fun DBufferGeometry.merge(other: DBufferGeometry): DBufferGeometry {
    val attrMap = mutableMapOf<String, BufferAttribute>()

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

    return DBufferGeometry(attrMap.values.toList())
}

fun BufferAttribute.merge(other: BufferAttribute): BufferAttribute {
    return BufferAttribute(attributeName, data + other.data, count)
}

fun List<Coords3d>.center(): Coords3d {
    if (isEmpty()) return Coords3d.ZERO

    var x = 0.0
    var y = 0.0
    var z = 0.0

    forEach {
        x += it.x
        y += it.y
        z += it.z
    }

    return Coords3d(
            x / size,
            y / size,
            z / size
    )
}

fun areaOf(rangeX: IntRange, rangeY: IntRange): Sequence<Pair<Int, Int>> {
    return rangeX.asSequence().flatMap { x -> rangeY.asSequence().map { y -> x to y } }
}

fun List<DPolygon>.toGeometry(): DGeometry {
    val coords = flatMap { it.triangles() }.flatMap { listOf(it.x, 0f, it.y) }

    return GeometryBuilder.build(coords)
}

fun DArea.toSQL(): String {
    val minX = pos.x
    val minY = pos.y
    val maxX = pos.x + size.x
    val maxY = pos.y + size.y

    return "ST_GeomFromText('POLYGON(($minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY))')"
}

fun org.postgis.Polygon.toPolygon(): DPolygon {
    val points = getRing(0).points.map { Vector2(it.x.toFloat(), it.y.toFloat()) }
//    val holes = (1 until numRings()).map { getRing(it).points.reversed().map { Vector2(it.x.toFloat(), it.y.toFloat()) } }

    return DPolygon(points, emptyList())
}

fun DPolygon.relativize(): DPolygon {
    return DPolygon(
            points.map { it.relativize() },
            holes.map { it.map { it.relativize() } }
    )
}

fun Vector2.relativize(): Vector2 {
    return Vector2(
            x - TerrainLoader.ORIGIN.x,
            -(y - TerrainLoader.ORIGIN.z)
    )
}

fun colorFromHue(hue: Float): DColor {
    val c = java.awt.Color.getHSBColor(hue, 0.5f, 1f)
    return DColor(c.red / 255f, c.green / 255f, c.blue / 255f)
}

infix fun Int.upTo(other: Int): IntRange = this until other