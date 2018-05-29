package com.cout970.server.rest

import com.cout970.server.glTF.*
import com.google.gson.GsonBuilder
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

typealias Vector4 = Vector4f
typealias Vector3 = Vector3f
typealias Vector2 = Vector2f
typealias Polygon3d = eu.printingin3d.javascad.vrl.Polygon

data class Triangle2d(
        val a: Vector2,
        val b: Vector2,
        val c: Vector2
)

val SCENE_GSON = GsonBuilder()
        .registerTypeAdapter(Vector4::class.java, Vector4Serializer())
        .registerTypeAdapter(Vector3::class.java, Vector3Serializer())
        .registerTypeAdapter(Vector2::class.java, Vector2Serializer())
        .registerTypeAdapter(Quaternion::class.java, QuaternionSerializer())
        .registerTypeAdapter(Matrix4::class.java, Matrix4Serializer())
        .registerTypeAdapter(DColor::class.java, ColorSerializer())
        .setPrettyPrinting()
        .create()

// TSDL
// Scene  Definition Language


data class DRotation(
        val angle: Float,
        val axis: Vector3
)

data class DColor(
        val r: Float,
        val g: Float,
        val b: Float
)

data class DPolygon(
        val points: List<Vector2>,
        val holes: List<List<Vector2>> = emptyList()
)

data class BufferAttribute(
        val attributeName: String,
        val data: FloatArray,
        val count: Int
)

data class DGeometry(
        val attributes: List<BufferAttribute>
)

data class DMaterial(
        val ambientIntensity: Float,
        val shininess: Float,
        val diffuseColor: DColor,
        val emissiveColor: DColor,
        val specularColor: DColor,
        val transparency: Float
)

data class DModel(
        val geometry: DGeometry,
        val material: DMaterial
)

enum class DCameraType { PERSPECTIVE, ORTHOGRAPHIC }

data class DViewPoint(
        val location: Vector3,
        val orientation: DRotation,
        val camera: DCameraType
)

// TODO
data class DGround(
        val resolution: Float,
        val exaggeration: Float,
        val material: DMaterial
//            val texture: TODO()
)

sealed class DGroundProjection {

    data class DefaultGroundProjection(
            val elevation: Float, // relative to the ground
            val top: Boolean
    ) : DGroundProjection()

    data class SnapProjection(
            val elevation: Float // relative to the ground
    ) : DGroundProjection()

    data class BridgeGroundProjection(
            val startElevation: Float,
            val endElevation: Float
    ) : DGroundProjection()
}

sealed class DShape {

    data class ShapeAtPoint(
            val model: DModel,
            val position: Vector3,
            val rotation: DRotation,
            val scale: Vector3,
            val projection: DGroundProjection
    ) : DShape()

    data class ShapeAtLine(
            val model: DModel,
            val lineStart: Vector3,
            val lineEnd: Vector3,
            val rotation: DRotation, // item rotation, not line rotation
            val scale: Vector3,
            val projection: DGroundProjection,
            val initialGap: Float,
            val gap: Float
    ) : DShape()

    data class ShapeAtSurface(
            val model: DModel,
            val surface: DPolygon,
            val rotation: DRotation,
            val scale: Vector3,
            val resolution: Float,
            val projection: DGroundProjection
    ) : DShape()

    data class ExtrudeSurface(
            val surface: DPolygon,
            val height: Float,
            val rotation: DRotation,
            val scale: Vector3,
            val material: DMaterial,
            val projection: DGroundProjection
    ) : DShape()

    data class BakedShape(
            val models: List<Pair<DMaterial, List<String>>>
    ) : DShape()
}

data class DLabel(
        val txt: String,
        val position: Vector3,
        val scale: Double
)

data class DRule(
        val filter: String,
        val minDistance: Float,
        val maxDistance: Float,
        val shapes: List<DShape>
)

data class DLayer(
        val name: String,
        val description: String,
        val rules: List<DRule>,
        val labels: List<DLabel>
)

data class DScene(
        val title: String,
        val abstract: String,
        val viewPoints: List<DViewPoint>,
//            val ground: Ground,
        val layers: List<DLayer>
)