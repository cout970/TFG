package com.cout970.server.scene

import com.cout970.server.glTF.*
import com.google.gson.GsonBuilder

typealias Polygon3d = eu.printingin3d.javascad.vrl.Polygon

data class Triangle2d(
        val a: Vector2,
        val b: Vector2,
        val c: Vector2
)

data class Model(
        val geometry: DGeometry,
        val material: DMaterial
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
// Terrain? Scene Definition Language

// Basic Components
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

data class DArea(
        val pos: Vector2,
        val size: Vector2
)

data class DMaterial(
        val metallic: Float,
        val roughness: Float,
        val diffuseColor: DColor,
        val emissiveColor: DColor,
        val opacity: Float
)

// Geomtries

sealed class DGeometry

data class DBufferGeometry(
        val attributes: List<BufferAttribute>
) : DGeometry()

@Deprecated("old")
data class BufferAttribute(
        val attributeName: String,
        val data: FloatArray,
        val count: Int
)

data class DTransformGeometry(
        val source: DGeometry,
        val translation: Vector3 = Vector3(),
        val rotation: DRotation = DRotation(0f, Vector3()),
        val scale: Vector3 = Vector3(1f)
) : DGeometry()

// DDBB accessors

sealed class DGeometrySource

data class DPolygonsSource(
        val geomField: String,
        val tableName: String,
        val area: DArea
) : DGeometrySource()

data class DExtrudedPolygonsSource(
        val geomField: String,
        val heightField: String,
        val tableName: String,
        val heightScale: Float,
        val area: DArea
) : DGeometrySource()

data class DInlineSource(
        val geometry: DGeometry
) : DGeometrySource()

data class DFileSource(
        val file: String
) : DGeometrySource()

// other DDBB accessors

data class DPointSource(
        val geomField: String,
        val tableName: String,
        val area: DArea
)

data class DLabelSource(
        val geomField: String,
        val textField: String,
        val tableName: String,
        val area: DArea
)

// Terrain Projection

sealed class DGroundProjection

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

// Shapes (Geometry processing)

sealed class DShape

data class ShapeAtPoint(
        val geometry: DGeometry,
        val point: Vector2,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShape()

data class ShapeAtLine(
        val geometry: DGeometry,
        val material: DMaterial,
        val lineStart: Vector3,
        val lineEnd: Vector3,
        val projection: DGroundProjection,
        val initialGap: Float,
        val gap: Float
) : DShape()

data class ShapeAtSurface(
        val geometry: DGeometry,
        val material: DMaterial,
        val surface: DPolygon,
        val resolution: Float,
        val projection: DGroundProjection
) : DShape()

data class ShapeExtrudeSurface(
        val surface: DPolygon,
        val height: Float,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShape()

data class ShapeLabel(
        val txt: String,
        val position: Vector2,
        val scale: Float,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShape()

// Definition of shapes

sealed class DShapeSource

data class DInlineShapeSource(
        val shape: DShape
) : DShapeSource()

data class DShapeAtPointSource(
        val geometrySource: DGeometrySource,
        val points: DPointSource,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

data class DShapeAtSurfaceSource(
        val geometrySource: DGeometrySource,
        val surfaceSource: DPolygonsSource,
        val resolution: Float,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

data class DExtrudedShapeSource(
        val polygonsSource: DExtrudedPolygonsSource,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

data class DExtrudeShapeSource(
        val polygonsSource: DPolygonsSource,
        val height: Float,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

data class DPolygonsShapeSource(
        val geometrySource: DPolygonsSource,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

data class DLabelShapeSource(
        val labelSource: DLabelSource,
        val scale: Float,
        val material: DMaterial,
        val projection: DGroundProjection
) : DShapeSource()

// Render rules

sealed class DProperty

data class DPropertyLOD(
        val minDistance: Float,
        val maxDistance: Float
) : DProperty()

object DPropertyFollowCamera : DProperty()

data class DRule(
        val properties: List<DProperty>,
        val shapes: List<DShapeSource>
)

// Layers

data class DLayer(
        val name: String,
        val description: String,
        val rules: List<DRule>
)

// Ground elevation files

data class DGround(
        val file: String,
        val material: DMaterial,
        val area: DArea,
        val gridSize: Float
)

// Cameras

enum class DCameraType { PERSPECTIVE, ORTHOGRAPHIC }

data class DViewPoint(
        val location: Vector3,
        val orientation: Vector3,
        val camera: DCameraType
)

// Final scene

data class DScene(
        val title: String,
        val abstract: String,
        val origin: Vector2,
        val ground: DGround,
        val viewPoints: List<DViewPoint>,
        val layers: List<DLayer>
)