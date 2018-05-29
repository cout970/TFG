package com.cout970.server.util

import com.cout970.server.glTF.GLTFBuilder
import com.cout970.server.glTF.gltfModel
import com.cout970.server.rest.*
import com.cout970.server.rest.DGroundProjection.*
import com.cout970.server.rest.DShape.BakedShape
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.collections.FloatArrayList
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

    fun bake2(scene: DScene) = gltfModel {
        bufferName = UUID.randomUUID().toString() + ".bin"

        scene.layers.forEach { layer ->

            scene {
                name = layer.name

                layer.rules.forEachIndexed { index, rule ->

                    node {
                        name = "rule $index"

                        rule.shapes.forEachIndexed { index, shape ->

                            node {
                                name = "shape $index"
                                shapeMesh(this, shape)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun GLTFBuilder.shapeMesh(node: GLTFBuilder.Node, shape: DShape) = node.apply {
        mesh {
            val baked = bakeShape(shape)
            baked.models.forEach { (mat, geoms) ->

                // TODO add material
                geoms.forEach { geom ->

                    primitive {

                        val data: FloatArray = Rest.cacheMap[geom]!!
                        val vertices = data.toList().windowed(3, 3).map { Vector3(it[0], it[1], it[2]) }

                        attributes[POSITION] = buffer(FLOAT, vertices)
                    }
                }
            }
        }
    }

    fun bake(scene: DScene): DScene {
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

    fun bakeShapes(shapes: List<DShape>): BakedShape {
        return shapes.parallelStream()
                .map {
                    try {
                        Optional.of(SceneBaker.bakeShape(it))
                    } catch (e: Exception) {
//                      e.printStackTrace()
                        Optional.empty<BakedShape>()
                    }
                }
                .filter { it.isPresent }
                .map { it.get() }
                .reduce { a: BakedShape, b: BakedShape -> a.merge(b) }
                .get()
    }

    fun bakeShape(shape: DShape): BakedShape = when (shape) {
        is BakedShape -> shape
        is DShape.ShapeAtPoint -> bakeShapeAtPoint(shape)
        is DShape.ShapeAtLine -> bakeShapeAtLine(shape)
        is DShape.ShapeAtSurface -> bakeShapeAtSurface(shape)
        is DShape.ExtrudeSurface -> bakeExtrudeShape(shape)
    }

    private fun bakeShapeAtPoint(shape: DShape.ShapeAtPoint): BakedShape {
        val model = shape.model

        val newGeometry = model.geometry.transform(
                translation = shape.position,
                rotation = shape.rotation,
                scale = shape.scale,
                projection = shape.projection
        )

        return saveInCache(newGeometry, model.material)
    }

    private fun bakeShapeAtLine(shape: DShape.ShapeAtLine): BakedShape {
        val direction = (shape.lineEnd - shape.lineStart).normalize()
        val start = shape.lineStart + (direction * shape.initialGap)

        val line = shape.lineEnd - start
        val numPoints = sqrt(line.x * line.x + line.z * line.z).toInt()

        val geometries = mutableListOf<DGeometry>()

        repeat(numPoints) { i ->
            val point2d = start + (direction * shape.gap * i.toFloat())

            geometries += shape.model.geometry.transform(
                    translation = point2d,
                    rotation = shape.rotation,
                    scale = shape.scale,
                    projection = shape.projection
            )
        }

        val newGeometry = geometries.reduce { acc, geometry -> acc.merge(geometry) }
        return saveInCache(newGeometry, shape.model.material)
    }

    private fun bakeShapeAtSurface(shape: DShape.ShapeAtSurface): BakedShape {
        val locations = mutableListOf<Vector2>()
        val geometries = mutableListOf<DGeometry>()

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
            geometries += shape.model.geometry.transform(
                    translation = Vector3(point2d.x, 0f, point2d.y),
                    rotation = shape.rotation,
                    scale = shape.scale,
                    projection = shape.projection
            )
        }

        val newGeometry = geometries.reduce { acc, geometry -> acc.merge(geometry) }
        return saveInCache(newGeometry, shape.model.material)
    }

    private fun bakeExtrudeShape(shape: DShape.ExtrudeSurface): BakedShape {
        require(shape.surface.points.size >= 3) {
            "Polygon must have at least 3 points, it had ${shape.surface.points.size} instead"
        }

        val coords = shape.surface.points.map { Coords2d(it.x.toDouble(), it.y.toDouble()) }
        val polygon = Polygon(coords)
        val height = shape.height.toDouble()
        val model = LinearExtrude(polygon, height, Angle.ZERO, 1.0)

        val polygons = model.toCSG(FacetGenerationContext.DEFAULT).polygons

        val yOffset = height / 2
        val triangles = polygons.flatMap { Triangulator.triangulate(it) }.map { tri ->
            val (a, b, c) = tri.points
            Triangle3d(
                    Coords3d(a.x, a.z + yOffset, a.y),
                    Coords3d(b.x, b.z + yOffset, b.y),
                    Coords3d(c.x, c.z + yOffset, c.y)
            )
        }

        val newGeometry = triangles.toGeometry().transform(
                translation = Vector3(),
                rotation = shape.rotation,
                scale = shape.scale,
                projection = shape.projection
        )

        return saveInCache(newGeometry, shape.material)
    }

    private fun project(projection: DGroundProjection, points: FloatArray): FloatArray {

        when (projection) {
            is DefaultGroundProjection -> {
                var min = Float.MAX_VALUE
                var max = Float.MIN_VALUE

                points.indices.windowed(3, 3).forEach { i ->
                    val xPos = points[i[0]] // + TerrainLoader.ORIGIN.x
                    val zPos = points[i[2]] // + TerrainLoader.ORIGIN.z

                    val h = TerrainLoader.getHeight(xPos, zPos)
                    min = Math.min(min, h)
                    max = Math.max(max, h)
                }

                if (projection.top) {
                    points.indices.windowed(3, 3).forEach { i ->
                        points[i[1]] += max
                    }

                } else {
                    points.indices.windowed(3, 3).forEach { i ->
                        points[i[1]] += min
                    }
                }
            }
            is SnapProjection -> {
                points.indices.windowed(3, 3).forEach { i ->
                    val xPos = points[i[0]] //+ TerrainLoader.ORIGIN.x
                    val zPos = points[i[2]] //+ TerrainLoader.ORIGIN.z

                    points[i[1]] += TerrainLoader.getHeight(xPos, zPos)
                }
            }
            is BridgeGroundProjection -> TODO("")
        }

        return points
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

    private fun DGeometry.transform(translation: Vector3, rotation: DRotation, scale: Vector3, projection: DGroundProjection): DGeometry {
        val matrix = Matrix4f().apply {
            translate(translation)
            rotate(rotation.angle, rotation.axis)
            scale(scale)
        }

        val newAttributes = attributes.map { attr ->
            if (attr.attributeName != "position") return@map attr

            val data = expandTriangles(attr.data)

            val newData = FloatArray(data.size)
            val input = Vector4f(0f, 0f, 0f, 1f)
            val output = Vector4f(0f, 0f, 0f, 1f)

            repeat(data.size / 3) { i ->
                input.x = data[i * 3]
                input.y = data[i * 3 + 1]
                input.z = data[i * 3 + 2]

                matrix.transform(input, output)

                newData[i * 3] = output.x
                newData[i * 3 + 1] = output.y
                newData[i * 3 + 2] = output.z
            }

            attr.copy(data = project(projection, newData))
        }

        return copy(attributes = newAttributes)
    }

    private fun saveInCache(newGeometry: DGeometry, material: DMaterial): BakedShape {
        val str = UUID.randomUUID().toString()
        Rest.cacheMap[str] = newGeometry.attributes.find { it.attributeName == "position" }?.data!!
        return BakedShape(listOf(material to listOf(str)))
    }

    private fun expandTriangles(data: FloatArray): FloatArray {
        val list = FloatArrayList()
        val p0 = Vector3()
        val p1 = Vector3()
        val p2 = Vector3()

        repeat(data.size / 9) { i ->
            p0.x = data[i * 9]
            p0.y = data[i * 9 + 1]
            p0.z = data[i * 9 + 2]

            p1.x = data[i * 9 + 3]
            p1.y = data[i * 9 + 4]
            p1.z = data[i * 9 + 5]

            p2.x = data[i * 9 + 6]
            p2.y = data[i * 9 + 7]
            p2.z = data[i * 9 + 8]

            val p10 = p0 - p1
            val p20 = p0 - p2
            val p21 = p1 - p2

            if (p10.length() > 50 || p20.length() > 50 || p21.length() > 50) {

                val a = p10 * 0.5f + p1
                val b = p20 * 0.5f + p2
                val c = p21 * 0.5f + p2

                // left-down
                list.add(p0)
                list.add(a)
                list.add(b)

                //center
                list.add(a)
                list.add(c)
                list.add(b)

                // right-down
                list.add(b)
                list.add(c)
                list.add(p2)

                // top
                list.add(a)
                list.add(p1)
                list.add(c)

            } else {
                list.add(p0)
                list.add(p1)
                list.add(p2)
            }
        }
        return list.toFloatArray()
    }

    private fun FloatArrayList.add(i: Vector3) {
        add(i.x)
        add(i.y)
        add(i.z)
    }
}