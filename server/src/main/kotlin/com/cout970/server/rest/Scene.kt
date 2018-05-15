package com.cout970.server.rest

import org.joml.Vector2f
import org.joml.Vector3f

typealias Vector3 = Vector3f
typealias Vector2 = Vector2f
typealias Polygon3d = eu.printingin3d.javascad.vrl.Polygon

data class Triangle2d(
        val a: Vector2,
        val b: Vector2,
        val c: Vector2
)

object Defs {

    data class Rotation(
            val angle: Float,
            val axis: Vector3
    )

    data class Color(
            val r: Float,
            val g: Float,
            val b: Float
    )

    data class Polygon(
            val points: List<Vector2>,
            val holes: List<List<Vector2>> = emptyList()
    )

    data class BufferAttribute(
            val attributeName: String,
            val data: FloatArray,
            val count: Int
    )

    data class Geometry(
            val attributes: List<BufferAttribute>
    )

    data class Material(
            val ambientIntensity: Float,
            val shininess: Float,
            val diffuseColor: Color,
            val emissiveColor: Color,
            val specularColor: Color,
            val transparency: Float
    )

    data class Model(
            val geometry: Geometry,
            val material: Material
    )

    enum class CameraType { PERSPECTIVE, ORTHOGRAPHIC }

    data class ViewPoint(
            val location: Vector3,
            val orientation: Rotation,
            val camera: CameraType
    )

    // TODO
    data class Ground(
            val resolution: Float,
            val exaggeration: Float,
            val material: Material
//            val texture: TODO()
    )

    sealed class GroundProjection {

        data class DefaultGroundProjection(
                val elevation: Float, // relative to the ground
                val top: Boolean
        ) : GroundProjection()

        data class SnapProjection(
                val elevation: Float // relative to the ground
        ) : GroundProjection()

        data class BridgeGroundProjection(
                val startElevation: Float,
                val endElevation: Float
        ) : GroundProjection()
    }

    sealed class Shape {

        data class ShapeAtPoint(
                val model: Model,
                val position: Vector3,
                val rotation: Rotation,
                val scale: Vector3,
                val projection: GroundProjection
        ) : Shape()

        data class ShapeAtLine(
                val model: Model,
                val lineStart: Vector3,
                val lineEnd: Vector3,
                val rotation: Rotation, // item rotation, not line rotation
                val scale: Vector3,
                val projection: GroundProjection,
                val initialGap: Float,
                val gap: Float
        ) : Shape()

        data class ShapeAtSurface(
                val model: Model,
                val surface: Polygon,
                val rotation: Rotation,
                val scale: Vector3,
                val resolution: Float,
                val projection: GroundProjection
        ) : Shape()

        data class ExtrudeSurface(
                val surface: Polygon,
                val height: Float,
                val rotation: Rotation,
                val scale: Vector3,
                val material: Material,
                val projection: GroundProjection
        ) : Shape()

        data class BakedShape(
                val models: List<Pair<Material, List<String>>>
        ) : Shape()
    }

    data class Label(
            val txt: String,
            val position: Vector3,
            val scale: Double
    )

    data class Rule(
            val filter: String,
            val minDistance: Float,
            val maxDistance: Float,
            val shapes: List<Shape>
    )

    data class Layer(
            val name: String,
            val description: String,
            val rules: List<Rule>,
            val labels: List<Label>
    )

    data class Scene(
            val title: String,
            val abstract: String,
            val viewPoints: List<ViewPoint>,
//            val ground: Ground,
            val layers: List<Layer>
    )
}