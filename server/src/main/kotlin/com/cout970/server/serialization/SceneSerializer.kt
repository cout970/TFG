package com.cout970.server.serialization

import com.cout970.server.glTF.*
import com.cout970.server.scene.*
import com.google.gson.*
import java.lang.reflect.Type

val SCENE_GSON = GsonBuilder()
        .registerTypeAdapter(Vector4::class.java, Vector4Serializer())
        .registerTypeAdapter(Vector3::class.java, Vector3Serializer())
        .registerTypeAdapter(Vector2::class.java, Vector2Serializer())
        .registerTypeAdapter(Quaternion::class.java, QuaternionSerializer())
        .registerTypeAdapter(Matrix4::class.java, Matrix4Serializer())
        .registerTypeAdapter(DColor::class.java, ColorSerializer())
        .registerTypeAdapter(DScene::class.java, SceneSerializer())
        .registerTypeAdapter(DGround::class.java, GroundSerializer())
        .registerTypeAdapter(DMaterial::class.java, MaterialSerializer())
        .registerTypeAdapter(DViewPoint::class.java, ViewPointSerializer())
        .registerTypeAdapter(DLayer::class.java, LayerSerializer())
        .registerTypeAdapter(DRule::class.java, RuleSerializer())
        .registerTypeAdapter(DProperty::class.java, PropertySerializer())
        .registerTypeAdapter(DShapeSource::class.java, ShapeSourceSerializer())
        .registerTypeAdapter(DShape::class.java, ShapeSerializer())
        .setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .create()

private typealias JDC = JsonDeserializationContext
private typealias JSC = JsonSerializationContext

abstract class BaseSerializer<T> : JsonDeserializer<T>, JsonSerializer<T> {

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): T {
        return deserialize(json, context)
    }

    override fun serialize(src: T, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        return serialize(src, context)
    }

    abstract fun deserialize(json: JsonElement, ctx: JDC): T

    abstract fun serialize(json: T, ctx: JSC): JsonElement

}

class SceneSerializer : BaseSerializer<DScene>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DScene {
        val obj = json.asJsonObject
        return DScene(
                title = ctx.deserializeT(obj["title"]),
                origin = ctx.deserializeT(obj["origin"]),
                ground = ctx.deserializeT(obj["ground"]),
                layers = ctx.deserializeT(obj["layers"]),
                viewPoints = ctx.deserializeT(obj["viewPoints"]),
                abstract = ctx.deserializeT(obj["abstract"])
        )
    }

    override fun serialize(json: DScene, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class GroundSerializer : BaseSerializer<DGround>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DGround {
        val obj = json.asJsonObject
        return DGround(
                file = ctx.deserializeT(obj["file"]),
                material = ctx.deserializeT(obj["material"]),
                area = ctx.deserializeT(obj["area"]),
                gridSize = ctx.deserializeT(obj["gridSize"])
        )
    }

    override fun serialize(json: DGround, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class MaterialSerializer : BaseSerializer<DMaterial>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DMaterial {
        val obj = json.asJsonObject
        return DMaterial(
                metallic = ctx.deserializeT(obj["metallic"]),
                roughness = ctx.deserializeT(obj["roughness"]),
                diffuseColor = ctx.deserializeT(obj["diffuseColor"]),
                emissiveColor = ctx.deserializeT(obj["emissiveColor"]),
                opacity = ctx.deserializeT(obj["opacity"])
        )
    }

    override fun serialize(json: DMaterial, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class ViewPointSerializer : BaseSerializer<DViewPoint>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DViewPoint {
        val obj = json.asJsonObject
        return DViewPoint(
                location = ctx.deserializeT(obj["location"]),
                orientation = ctx.deserializeT(obj["orientation"]),
                camera = ctx.deserializeT(obj["camera"])
        )
    }

    override fun serialize(json: DViewPoint, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class LayerSerializer : BaseSerializer<DLayer>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DLayer {
        val obj = json.asJsonObject
        return DLayer(
                name = ctx.deserializeT(obj["name"]),
                description = ctx.deserializeT(obj["description"]),
                rules = ctx.deserializeT(obj["rules"])
        )
    }

    override fun serialize(json: DLayer, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class RuleSerializer : BaseSerializer<DRule>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DRule {
        val obj = json.asJsonObject
        return DRule(
                properties = ctx.deserializeT(obj["properties"]),
                shapes = ctx.deserializeT(obj["shapes"])
        )
    }

    override fun serialize(json: DRule, ctx: JSC): JsonElement = ctx.serializeT(scene)
}

class PropertySerializer : BaseSerializer<DProperty>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DProperty {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "LevelOfDetail" -> ctx.deserializeT<DPropertyLOD>(json)
            "FollowCamera" -> DPropertyFollowCamera
            else -> error("Unknown property type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DProperty, ctx: JSC): JsonElement {
        val type = when (json) {
            is DPropertyLOD -> "LevelOfDetail"
            DPropertyFollowCamera -> "FollowCamera"
        }

        return ctx.serialize(json).asJsonObject.apply { addProperty("type", type) }
    }
}

class ShapeSourceSerializer : BaseSerializer<DShapeSource>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DShapeSource {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "Inline" -> DInlineShapeSource(
                    shape = ctx.deserializeT(obj["shape"])
            )
            "ShapeAtPoint" -> DShapeAtPointSource(
                    geometrySource = ctx.deserializeT(obj["geometrySource"]),
                    points = ctx.deserializeT(obj["points"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "ShapeAtSurface" -> DShapeAtSurfaceSource(
                    geometrySource = ctx.deserializeT(obj["geometrySource"]),
                    surfaceSource = ctx.deserializeT(obj["surfaceSource"]),
                    resolution = ctx.deserializeT(obj["resolution"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "ExtrudedShape" -> DExtrudedShapeSource(
                    polygonsSource = ctx.deserializeT(obj["polygonsSource"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "ExtrudeShape" -> DExtrudeShapeSource(
                    polygonsSource = ctx.deserializeT(obj["polygonsSource"]),
                    height = ctx.deserializeT(obj["height"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "PolygonsShape" -> DPolygonsShapeSource(
                    geometrySource = ctx.deserializeT(obj["geometrySource"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "LabelShape" -> DLabelShapeSource(
                    labelSource = ctx.deserializeT(obj["labelSource"]),
                    scale = ctx.deserializeT(obj["scale"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            else -> error("Unknown property type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DShapeSource, ctx: JSC): JsonElement {
        val type = when(json){
            is DInlineShapeSource -> "Inline"
            is DShapeAtPointSource -> "ShapeAtPoint"
            is DShapeAtSurfaceSource -> "ShapeAtSurface"
            is DExtrudedShapeSource -> "ExtrudedShape"
            is DExtrudeShapeSource -> "ExtrudeShape"
            is DPolygonsShapeSource -> "PolygonsShape"
            is DLabelShapeSource -> "LabelShape"
        }
        return ctx.serializeT(scene).asJsonObject.apply { addProperty("type", type) }
    }
}

class ShapeSerializer : BaseSerializer<DShape>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DShape {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "ShapeAtPoint" -> ShapeAtPoint(
                    geometry = ctx.deserializeT(obj["geometry"]),
                    point = ctx.deserializeT(obj["point"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "ShapeAtLine" -> ShapeAtLine(
                    geometry = ctx.deserializeT(obj["geometry"]),
                    material = ctx.deserializeT(obj["material"]),
                    lineStart = ctx.deserializeT(obj["lineStart"]),
                    lineEnd = ctx.deserializeT(obj["lineEnd"]),
                    projection = ctx.deserializeT(obj["projection"]),
                    initialGap = ctx.deserializeT(obj["initialGap"]),
                    gap = ctx.deserializeT(obj["gap"])
            )
            "ShapeAtSurface" -> ShapeAtSurface(
                    geometry = ctx.deserializeT(obj["geometry"]),
                    material = ctx.deserializeT(obj["material"]),
                    surface = ctx.deserializeT(obj["surface"]),
                    resolution = ctx.deserializeT(obj["resolution"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "ExtrudeSurface" -> ShapeExtrudeSurface(
                    surface = ctx.deserializeT(obj["surface"]),
                    height = ctx.deserializeT(obj["height"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            "Label" -> ShapeLabel(
                    txt = ctx.deserializeT(obj["txt"]),
                    position = ctx.deserializeT(obj["position"]),
                    scale = ctx.deserializeT(obj["scale"]),
                    material = ctx.deserializeT(obj["material"]),
                    projection = ctx.deserializeT(obj["projection"])
            )
            else -> error("Unknown property type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DShape, ctx: JSC): JsonElement {
        val type = when(json){
            is ShapeAtPoint -> "ShapeAtPoint"
            is ShapeAtLine -> "ShapeAtLine"
            is ShapeAtSurface -> "ShapeAtSurface"
            is ShapeExtrudeSurface -> "ExtrudeSurface"
            is ShapeLabel -> "Label"
        }
        return ctx.serializeT(scene).asJsonObject.apply { addProperty("type", type) }
    }
}

class GroundProjectionSerializer : BaseSerializer<DGroundProjection>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DGroundProjection {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "Default" -> DefaultGroundProjection(
                    elevation = ctx.deserializeT(obj["elevation"]),
                    top = ctx.deserializeT(obj["top"])
            )
            "Snap" -> SnapProjection(
                    elevation = ctx.deserializeT(obj["elevation"])
            )
            "Bridge" -> BridgeGroundProjection(
                    start = ctx.deserializeT(obj["start"]),
                    end = ctx.deserializeT(obj["end"])
            )
            else -> error("Unknown property type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DGroundProjection, ctx: JSC): JsonElement {
        val type = when(json){
            is DefaultGroundProjection -> "Default"
            is SnapProjection -> "Snap"
            is BridgeGroundProjection -> "Bridge"
        }
        return ctx.serializeT(scene).asJsonObject.apply { addProperty("type", type) }
    }
}

