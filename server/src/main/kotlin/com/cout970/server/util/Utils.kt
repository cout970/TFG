package com.cout970.server.util

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Shape.BakedShape
import com.cout970.server.rest.Rest
import com.cout970.server.rest.Vector2
import com.cout970.server.terrain.TerrainLoader
import eu.printingin3d.javascad.coords.Coords3d
import java.util.*


inline fun (() -> Unit).ifFail(func: () -> Unit) {
    try {
        this()
    } catch (e: Exception) {
        e.printStackTrace()
        func()
    }
}

fun earthToScene(pos: Pair<Float, Float>): Pair<Float, Float> {
    val minX = TerrainLoader.ORIGIN.x
    val minY = TerrainLoader.ORIGIN.z

    return -(pos.first - minX) to (pos.second - minY)
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

fun Polygon.flip(): Polygon {
    return Polygon(points.reversed())
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

//fun BakedShape.merge(other: BakedShape): BakedShape {
//    val geometry = this.model.geometry.merge(other.model.geometry)
//    return BakedShape(Defs.Model(geometry, this.model.material))
//}

fun BakedShape.merge(other: BakedShape): BakedShape {
    val map = mutableMapOf<Defs.Material, List<String>>()

    other.models.forEach { map += it.first to it.second }

    models.forEach { (key, list) ->
        if (key !in map) {
            map += key to list
        } else {
            map[key] = (map[key]!!) + list
        }
    }

    val res = map.mapValues {
        listOf(it.value.reduce { acc, s ->
            val newStr = UUID.randomUUID().toString()

            val a = Rest.cacheMap.remove(acc)!!
            val b = Rest.cacheMap.remove(s)!!

            Rest.cacheMap[newStr] = a + b
            newStr
        })
    }.toList()

    return BakedShape(res)
}

fun areaOf(rangeX: IntRange, rangeY: IntRange): Sequence<Pair<Int, Int>> {
    return rangeX.asSequence().flatMap { x -> rangeY.asSequence().map { y -> x to y } }
}

fun List<Polygon>.toGeometry(): Defs.Geometry {
    val coords = flatMap { it.triangles() }.flatMap { listOf(it.x, 1f, it.y) }

    return MeshBuilder.buildGeometry(coords)
}

fun getAreaString(pos: Pair<Int, Int>): String {
    val ORIGIN = TerrainLoader.ORIGIN

    val minX = ORIGIN.x + (-4) * 1000
    val minY = ORIGIN.z + (-4) * 1000
    val maxX = ORIGIN.x + (4) * 1000
    val maxY = ORIGIN.z + (4) * 1000

    return "ST_GeomFromText('POLYGON(($minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY))')"
}

fun org.postgis.Polygon.toPolygon(): Polygon {
    val points = getRing(0).points.map { Vector2(it.x.toFloat(), it.y.toFloat()) }
    val holes = (2..numRings()).map { getRing(it - 1).points.map { Vector2(it.x.toFloat(), it.y.toFloat()) } }

    return Polygon(points, holes)
}

fun Polygon.relativize(): Polygon {
    // .flip()
    return Polygon(
            points.map { it.relativize() },
            holes.map { it.map { it.relativize() } }
    )
//        return Polygon(points.map { Vector2(it.x, it.y) })
}

fun Vector2.relativize(): Vector2 {
    return Vector2(
            x - TerrainLoader.ORIGIN.x,
            -(y - TerrainLoader.ORIGIN.z)
    )
}

fun colorFromHue(hue: Float): Defs.Color {
    val c = java.awt.Color.getHSBColor(hue, 0.5f, 1f)
    return Defs.Color(c.red / 255f, c.green / 255f, c.blue / 255f)
}

infix fun Int.upTo(other: Int): IntRange = this until other