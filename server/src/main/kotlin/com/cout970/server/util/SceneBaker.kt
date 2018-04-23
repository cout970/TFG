package com.cout970.server.util

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.Geometry
import com.cout970.server.rest.Defs.GroundProjection
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Defs.Shape.BakedShape
import com.cout970.server.rest.Rest
import com.cout970.server.rest.Vector2
import com.cout970.server.rest.Vector3
import eu.printingin3d.javascad.basic.Angle
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models.LinearExtrude
import eu.printingin3d.javascad.models2d.Polygon
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import org.joml.Matrix4f
import org.joml.Vector4f
import java.util.*
import kotlin.math.sqrt

object SceneBaker {

    fun bake(scene: Scene): Scene {
        val newLayers = scene.layers.map { layer ->
            val newRules = layer.rules.map { rule ->
                val newShapes = rule.shapes.mapNotNull { shape ->
                    try {
                        bakeShape(shape)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                rule.copy(shapes = newShapes)
            }
            layer.copy(rules = newRules)
        }
        return scene.copy(layers = newLayers)
    }

    fun bakeShape(shape: Shape): BakedShape = when (shape) {
        is BakedShape -> shape
        is Shape.ShapeAtPoint -> bakeShapeAtPoint(shape)
        is Shape.ShapeAtLine -> bakeShapeAtLine(shape)
        is Shape.ShapeAtSurface -> bakeShapeAtSurface(shape)
        is Shape.ExtrudeSurface -> bakeExtrudeShape(shape)
    }

    private fun bakeShapeAtPoint(shape: Shape.ShapeAtPoint): BakedShape {
        val model = shape.model
        val newGeometry = model.geometry.transform(
                translation = shape.position,
                rotation = shape.rotation,
                scale = shape.scale
        )

        return saveInCache(newGeometry, model.material)
    }

    private fun bakeShapeAtLine(shape: Shape.ShapeAtLine): BakedShape {
        val direction = (shape.lineEnd - shape.lineStart).normalize()
        val start = shape.lineStart + (direction * shape.initialGap)

        val line = shape.lineEnd - start
        val numPoints = sqrt(line.x * line.x + line.z * line.z).toInt()

        val geometries = mutableListOf<Geometry>()
        val projection = shape.projection

        repeat(numPoints) { i ->
            val point2d = start + (direction * shape.gap * i.toFloat())
            val point = project(projection, point2d)

            geometries += shape.model.geometry.transform(
                    translation = point,
                    rotation = shape.rotation,
                    scale = shape.scale
            )
        }

        val newGeometry = geometries.reduce { acc, geometry -> acc.merge(geometry) }
        return saveInCache(newGeometry, shape.model.material)
    }

    private fun bakeShapeAtSurface(shape: Shape.ShapeAtSurface): BakedShape {
        val locations = mutableListOf<Vector2>()
        val geometries = mutableListOf<Geometry>()

        val triangles = Triangulator.triangulate(shape.surface)

        triangles.forEach { (point1, point2, point3) ->

            // http://mathworld.wolfram.com/TrianglePointPicking.html
            val v0 = Vector2(0f)
            val v1 = point2 - point1
            val v2 = point3 - point1

            val area = getArea(v0, v1, v2)
            val elements = (area * shape.resolution * 2).toInt()

            repeat(elements) {
                val a1 = Math.random().toFloat()
                val a2 = Math.random().toFloat()
                val newPoint = v1 * a1 + v2 * a2

                if (inTriangle(newPoint, v0, v1, v2)) {
                    locations += (newPoint + point1)
                }
            }
        }

        locations.forEach { point2d ->
            val point = project(shape.projection, Vector3(point2d.x, 0f, point2d.y))

            geometries += shape.model.geometry.transform(
                    translation = point,
                    rotation = shape.rotation,
                    scale = shape.scale
            )
        }

        val newGeometry = geometries.reduce { acc, geometry -> acc.merge(geometry) }
        return saveInCache(newGeometry, shape.model.material)
    }

    private fun bakeExtrudeShape(shape: Shape.ExtrudeSurface): BakedShape {
        require(shape.surface.points.size >= 3) {
            "Polygon must have at least 3 points, it had ${shape.surface.points.size} instead"
        }

        val coords = shape.surface.points.map { Coords2d(it.x.toDouble(), it.y.toDouble()) }
        val polygon = Polygon(coords)
        val model = LinearExtrude(polygon, shape.height.toDouble(), Angle.ZERO, 1.0)

        val polygons = model.toCSG(FacetGenerationContext.DEFAULT).polygons

        val triangles = polygons.flatMap { Triangulator.triangulate(it) }.map { tri ->
            val (a, b, c) = tri.points
            Triangle3d(
                    Coords3d(a.x, a.z, a.y),
                    Coords3d(b.x, b.z, b.y),
                    Coords3d(c.x, c.z, c.y)
            )
        }

        val point = triangles.flatMap { it.points }.center()

        val yPos = project(shape.projection, Vector3(point.x.toFloat(), 0f, point.z.toFloat())).y

        val newGeometry = triangles.toGeometry().transform(
                translation = Vector3(0f, yPos, 0f),
                rotation = shape.rotation,
                scale = shape.scale
        )

        return saveInCache(newGeometry, shape.material)
    }

    private fun project(projection: GroundProjection, point: Vector3): Vector3 {
        val xPos = point.x + TerrainLoader.ORIGIN.x
        val zPos = point.z + TerrainLoader.ORIGIN.z
        val height = TerrainLoader.getHeight(xPos, zPos)

        // TODO fix ground projection
        val y = when (projection) {
            is DefaultGroundProjection -> height + projection.elevation
            else -> height
        }

        return Vector3(point.x, y, point.z)
    }

    //https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
    private fun inTriangle(p: Vector2, p0: Vector2, p1: Vector2, p2: Vector2): Boolean {
        val area = getArea(p0, p1, p2)
        val s = 1 / (2 * area) * (p0.y * p2.x - p0.x * p2.y + (p2.y - p0.y) * p.x + (p0.x - p2.x) * p.y)
        val t = 1 / (2 * area) * (p0.x * p1.y - p0.y * p1.x + (p0.y - p1.y) * p.x + (p1.x - p0.x) * p.y)
        return s > 0 && t > 0 && t + s <= 1
    }

    private fun getArea(p0: Vector2, p1: Vector2, p2: Vector2): Float {
        return 0.5f * (-p1.y * p2.x + p0.y * (-p1.x + p2.x) + p0.x * (p1.y - p2.y) + p1.x * p2.y)
    }

    private fun Geometry.transform(translation: Vector3, rotation: Rotation, scale: Vector3): Geometry {
        val matrix = Matrix4f().apply {
            translate(translation)
            rotate(rotation.angle, rotation.axis)
            scale(scale)
        }

        val newAttributes = attributes.map { attr ->
            if (attr.attributeName != "position") return@map attr

            val newData = FloatArray(attr.data.size)
            val input = Vector4f(0f, 0f, 0f, 1f)
            val output = Vector4f(0f, 0f, 0f, 1f)

            repeat(attr.data.size / 3) { i ->
                input.x = attr.data[i * 3]
                input.y = attr.data[i * 3 + 1]
                input.z = attr.data[i * 3 + 2]

                matrix.transform(input, output)

                newData[i * 3] = output.x
                newData[i * 3 + 1] = output.y
                newData[i * 3 + 2] = output.z
            }

            attr.copy(data = newData)
        }

        return copy(attributes = newAttributes)
    }

    private fun saveInCache(newGeometry: Geometry, material: Defs.Material): BakedShape {
        val str = UUID.randomUUID().toString()
        Rest.cacheMap[str] = newGeometry.attributes.find { it.attributeName == "position" }?.data!!
        return BakedShape(listOf(material to listOf(str)))
    }
}