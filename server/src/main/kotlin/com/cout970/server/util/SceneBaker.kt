package com.cout970.server.util

import com.cout970.server.rest.Defs.Geometry
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.Model
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Defs.Shape.BakedShape
import com.cout970.server.rest.Vector3
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.sqrt

object SceneBaker {

    fun bake(scene: Scene): Scene {
        val newLayers = scene.layers.map { layer ->
            val newRules = layer.rules.map { rule ->
                val newShapes = rule.shapes.map { shape ->
                    bakeShape(shape)
                }
                rule.copy(shapes = newShapes)
            }
            layer.copy(rules = newRules)
        }
        return scene.copy(layers = newLayers)
    }

    private fun bakeShape(shape: Shape): BakedShape = when (shape) {
        is BakedShape -> shape
        is Shape.ShapeAtPoint -> bakeShapeAtPoint(shape)
        is Shape.ShapeAtLine -> bakeShapeAtLine(shape)
        else -> TODO("only ShapeAtPoint and ShapeAtLine are done")
    }

    private fun bakeShapeAtPoint(shape: Shape.ShapeAtPoint): BakedShape {
        val model = shape.model
        val newGeometry = model.geometry.transform(
                translation = shape.position,
                rotation = shape.rotation,
                scale = shape.scale
        )

        val newModel = Model(newGeometry, model.material)
        return BakedShape(newModel)
    }

    private fun bakeShapeAtLine(shape: Shape.ShapeAtLine): BakedShape {
        val direction = (shape.lineEnd - shape.lineStart).normalize()
        val start = shape.lineStart + (direction * shape.initialGap)

        val line = shape.lineEnd - start
        val numPoints = sqrt(line.x * line.x + line.z * line.z).toInt()

        val geometries = mutableListOf<Geometry>()
        val projection = shape.projection

        repeat(numPoints) { i ->
            val point = start + (direction * shape.gap * i.toFloat())

            when (projection) {
                is DefaultGroundProjection -> {
                    point.y = TerrainLoader.getHeight(point.x + TerrainLoader.ORIGIN.x, point.z + TerrainLoader.ORIGIN.z) + projection.elevation
                }
                else -> { // TODO fix ground projection
                    point.y = TerrainLoader.getHeight(point.x, point.z)
                }
            }

            geometries += shape.model.geometry.transform(
                    translation = point,
                    rotation = shape.rotation,
                    scale = shape.scale
            )
        }

        val newGeometry = geometries.reduce { acc, geometry -> acc.merge(geometry) }

        val newModel = Model(newGeometry, shape.model.material)
        return BakedShape(newModel)
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
}