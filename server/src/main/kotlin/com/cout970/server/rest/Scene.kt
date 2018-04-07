package com.cout970.server.rest

import org.joml.Vector3f

typealias Vector3 = Vector3f

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

    data class Material(
            val ambientIntensity: Float,
            val shininess: Float,
            val diffuseColor: Color,
            val emissiveColor: Color,
            val specularColor: Color,
            val transparency: Float
    )

    data class BufferAttribute(val attributeName: String, val data: FloatArray, val count: Int)

    data class Geometry(val attributes: List<BufferAttribute>)

    enum class CameraType { PERSPECTIVE, ORTHOGRAFIC }

    data class ViewPoint(
            val location: Vector3,
            val orientation: Rotation,
            val camera: CameraType
    )

    data class Ground(
            val resolution: Float,
            val exaggeration: Float,
            val material: Material
//            val texture: TODO()
    )

    data class Model(val geometry: Geometry, val material: Material)

    sealed class GroundProjection {

        data class DefaultGroundProjection(
                val elevation: Float // relative to the ground
        ) : GroundProjection()

        data class HorizontalGroundProjection(
                val elevation: Float,
                val fromMax: Boolean // Using the top or the botton to apply the relative height
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
                val scale: Vector3
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
                val geometry: Model,
                val rotation: Rotation,
                val scale: Vector3,
                val resolution: Float,
                val projection: GroundProjection
        ) : Shape()

        // TODO
//        data class Extrude(
//                val model: Model,
//                val rotation: Rotation,
//                val scale: Vector3,
//                val resolution: Float,
//                val projection: GroundProjection
//        ) : Shape()
    }

    data class Rule(
            val filter: String,
            val minDistance: Float,
            val maxDistance: Float,
            val shapes: List<Shape>
    )

    data class Layer(
            val name: String,
            val description: String,
            val rules: List<Rule>
    )

    data class Scene(
            val title: String,
            val abstract: String,
            val viewPoints: List<ViewPoint>,
//            val ground: Ground,
            val layers: List<Layer>
    )
}