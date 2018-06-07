package com.cout970.server.serialization

import com.cout970.server.glTF.*
import com.cout970.server.scene.*
import com.google.gson.*
import java.awt.Color
import java.lang.reflect.Type

val SCENE_GSON = GsonBuilder()
        .registerTypeAdapter(Vector4::class.java, Vector4Serializer())
        .registerTypeAdapter(Vector3::class.java, Vector3Serializer())
        .registerTypeAdapter(Vector2::class.java, Vector2Serializer())
        .registerTypeAdapter(Quaternion::class.java, QuaternionSerializer())
        .registerTypeAdapter(Matrix4::class.java, Matrix4Serializer())
        .registerTypeAdapter(DScene::class.java, SceneSerializer())
        .registerTypeAdapter(DGround::class.java, GroundSerializer())
        .registerTypeAdapter(DMaterial::class.java, MaterialSerializer())
        .registerTypeAdapter(DViewPoint::class.java, ViewPointSerializer())
        .registerTypeAdapter(DLayer::class.java, LayerSerializer())
        .registerTypeAdapter(DRule::class.java, RuleSerializer())
        .registerTypeAdapter(DProperty::class.java, PropertySerializer())
        .registerTypeAdapter(DShapeSource::class.java, ShapeSourceSerializer())
        .registerTypeAdapter(DShape::class.java, ShapeSerializer())
        .registerTypeAdapter(DGroundProjection::class.java, GroundProjectionSerializer())
        .registerTypeAdapter(DPointSource::class.java, PointSourceSerializer())
        .registerTypeAdapter(DLabelSource::class.java, LabelSourceSerializer())
        .registerTypeAdapter(DGeometrySource::class.java, GeometrySourceSerializer())
        .registerTypeAdapter(DGeometry::class.java, GeometrySerializer())
        .registerTypeAdapter(DArea::class.java, AreaSerializer())
        .registerTypeAdapter(DPolygon::class.java, PolygonSerializer())
        .registerTypeAdapter(DColor::class.java, ColorSerializer())
        .registerTypeAdapter(DRotation::class.java, RotationSerializer())
        .setPrettyPrinting()
        .create()

private typealias JDC = JsonDeserializationContext
private typealias JSC = JsonSerializationContext

open class BaseSerializer<T> : JsonDeserializer<T>, JsonSerializer<T> {

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): T {
        return deserialize(json, context)
    }

    override fun serialize(src: T, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        return serialize(src, context)
    }

    open fun deserialize(json: JsonElement, ctx: JDC): T = TODO()
    open fun serialize(json: T, ctx: JSC): JsonElement = TODO()
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

    override fun serialize(json: DScene, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("title", json.title)
        add("origin", ctx.serializeT(json.origin))
        add("ground", ctx.serializeT(json.ground))
        add("layers", ctx.serializeT(json.layers))
        add("viewPoints", ctx.serializeT(json.viewPoints))
        addProperty("abstract", json.abstract)
    }
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

    override fun serialize(json: DGround, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("file", json.file)
        add("material", ctx.serializeT(json.material))
        add("area", ctx.serializeT(json.area))
        addProperty("gridSize", json.gridSize)
    }
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

    override fun serialize(json: DMaterial, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("metallic", json.metallic)
        addProperty("roughness", json.roughness)
        add("diffuseColor", ctx.serializeT(json.diffuseColor))
        add("emissiveColor", ctx.serializeT(json.emissiveColor))
        addProperty("opacity", json.opacity)
    }
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

    override fun serialize(json: DViewPoint, ctx: JSC): JsonElement = JsonObject().apply {
        add("location", ctx.serializeT(json.location))
        add("orientation", ctx.serializeT(json.orientation))
        add("camera", ctx.serializeT(json.camera))
    }
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

    override fun serialize(json: DLayer, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("name", json.name)
        addProperty("description", json.description)
        add("rules", ctx.serializeT(json.rules))
    }
}

class RuleSerializer : BaseSerializer<DRule>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DRule {
        val obj = json.asJsonObject
        return DRule(
                properties = ctx.deserializeT(obj["properties"]),
                shapes = ctx.deserializeT(obj["shapes"])
        )
    }

    override fun serialize(json: DRule, ctx: JSC): JsonElement = JsonObject().apply {
        add("properties", ctx.serializeT(json.properties))
        add("shapes", ctx.serializeT(json.shapes))
    }
}

class PropertySerializer : BaseSerializer<DProperty>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DProperty {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "LevelOfDetail" -> ctx.deserializeT<DPropertyLOD>(json)
            "FollowCamera" -> DPropertyFollowCamera
            else -> error("Unknown Property type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DProperty, ctx: JSC): JsonElement {
        val type = when (json) {
            is DPropertyLOD -> "LevelOfDetail"
            DPropertyFollowCamera -> "FollowCamera"
        }

        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
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
            else -> error("Unknown ShapeSource type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DShapeSource, ctx: JSC): JsonElement {
        val type = when (json) {
            is DInlineShapeSource -> "Inline"
            is DShapeAtPointSource -> "ShapeAtPoint"
            is DShapeAtSurfaceSource -> "ShapeAtSurface"
            is DExtrudedShapeSource -> "ExtrudedShape"
            is DExtrudeShapeSource -> "ExtrudeShape"
            is DPolygonsShapeSource -> "PolygonsShape"
            is DLabelShapeSource -> "LabelShape"
        }
        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
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
            else -> error("Unknown Shape type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DShape, ctx: JSC): JsonElement {
        val type = when (json) {
            is ShapeAtPoint -> "ShapeAtPoint"
            is ShapeAtLine -> "ShapeAtLine"
            is ShapeAtSurface -> "ShapeAtSurface"
            is ShapeExtrudeSurface -> "ExtrudeSurface"
            is ShapeLabel -> "Label"
        }
        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
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
            else -> error("Unknown GroundProjection type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DGroundProjection, ctx: JSC): JsonElement {
        val type = when (json) {
            is DefaultGroundProjection -> "Default"
            is SnapProjection -> "Snap"
            is BridgeGroundProjection -> "Bridge"
        }
        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
    }
}

class PointSourceSerializer : BaseSerializer<DPointSource>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DPointSource {
        val obj = json.asJsonObject
        return DPointSource(
                geomField = ctx.deserializeT(obj["geomField"]),
                tableName = ctx.deserializeT(obj["tableName"]),
                area = ctx.deserializeT(obj["area"])
        )
    }

    override fun serialize(json: DPointSource, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("geomField", json.geomField)
        addProperty("tableName", json.tableName)
        add("area", ctx.serializeT(json.area))
    }
}

class LabelSourceSerializer : BaseSerializer<DLabelSource>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DLabelSource {
        val obj = json.asJsonObject
        return DLabelSource(
                geomField = ctx.deserializeT(obj["geomField"]),
                textField = ctx.deserializeT(obj["textField"]),
                tableName = ctx.deserializeT(obj["tableName"]),
                area = ctx.deserializeT(obj["area"])
        )
    }

    override fun serialize(json: DLabelSource, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("geomField", json.geomField)
        addProperty("textField", json.textField)
        addProperty("tableName", json.tableName)
        add("area", ctx.serializeT(json.area))
    }
}

class GeometrySourceSerializer : BaseSerializer<DGeometrySource>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DGeometrySource {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "Polygon" -> DPolygonsSource(
                    geomField = ctx.deserializeT(obj["geomField"]),
                    tableName = ctx.deserializeT(obj["tableName"]),
                    area = ctx.deserializeT(obj["area"])
            )
            "ExtrudedPolygon" -> DExtrudedPolygonsSource(
                    geomField = ctx.deserializeT(obj["geomField"]),
                    heightField = ctx.deserializeT(obj["heightField"]),
                    tableName = ctx.deserializeT(obj["tableName"]),
                    heightScale = ctx.deserializeT(obj["heightScale"]),
                    area = ctx.deserializeT(obj["area"])
            )
            "Inline" -> DInlineSource(
                    geometry = ctx.deserializeT(obj["geometry"])
            )
            "File" -> DFileSource(
                    file = ctx.deserializeT(obj["file"])
            )
            else -> error("Unknown GeometrySource type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DGeometrySource, ctx: JSC): JsonElement {
        val type = when (json) {
            is DPolygonsSource -> "Polygon"
            is DExtrudedPolygonsSource -> "ExtrudedPolygon"
            is DInlineSource -> "Inline"
            is DFileSource -> "File"
        }
        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
    }
}

class GeometrySerializer : BaseSerializer<DGeometry>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DGeometry {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "Buffer" -> DBufferGeometry(
                    attributes = ctx.deserializeT(obj["attributes"])
            )
            "Transform" -> DTransformGeometry(
                    source = ctx.deserializeT(obj["source"]),
                    translation = ctx.deserializeT(obj["translation"]),
                    rotation = ctx.deserializeT(obj["rotation"]),
                    scale = ctx.deserializeT(obj["scale"])
            )
            else -> error("Unknown Geometry type: ${obj["type"].asString}")
        }
    }

    override fun serialize(json: DGeometry, ctx: JSC): JsonElement {
        val type = when (json) {
            is DBufferGeometry -> "Buffer"
            is DTransformGeometry -> "Transform"
        }
        return ctx.serialize(json, json::class.java).asJsonObject.apply { addProperty("type", type) }
    }
}

class AreaSerializer : BaseSerializer<DArea>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DArea {
        val obj = json.asJsonObject
        return DArea(
                pos = ctx.deserializeT(obj["pos"]),
                size = ctx.deserializeT(obj["size"])
        )
    }

    override fun serialize(json: DArea, ctx: JSC): JsonElement = JsonObject().apply {
        add("pos", ctx.serializeT(json.pos))
        add("size", ctx.serializeT(json.size))
    }
}

class PolygonSerializer : BaseSerializer<DPolygon>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DPolygon {
        val obj = json.asJsonObject
        return DPolygon(
                points = ctx.deserializeT(obj["points"]),
                holes = ctx.deserializeT(obj["holes"])
        )
    }

    override fun serialize(json: DPolygon, ctx: JSC): JsonElement = JsonObject().apply {
        add("points", ctx.serializeT(json.points))
        add("holes", ctx.serializeT(json.holes))
    }
}

class ColorSerializer : JsonSerializer<DColor>, JsonDeserializer<DColor> {

    override fun serialize(src: DColor, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val str = Integer.toHexString(Color(src.r, src.g, src.b, 1f).rgb).run {
            substring(2, length)
        }
        return JsonPrimitive(str)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DColor {
        return colorOf(json.asString)
    }

}

class RotationSerializer : BaseSerializer<DRotation>() {

    override fun deserialize(json: JsonElement, ctx: JDC): DRotation {
        val obj = json.asJsonObject
        return DRotation(
                angle = ctx.deserializeT(obj["angle"]),
                axis = ctx.deserializeT(obj["axis"])
        )
    }

    override fun serialize(json: DRotation, ctx: JSC): JsonElement = JsonObject().apply {
        addProperty("angle", json.angle)
        add("axis", ctx.serializeT(json.axis))
    }
}
