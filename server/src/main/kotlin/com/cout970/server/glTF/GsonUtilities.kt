package com.cout970.server.glTF

import com.cout970.server.rest.DColor
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.joml.Quaternionf
import java.awt.Color
import java.lang.reflect.Type
import java.util.*

/**
 * Created by cout970 on 2017/01/04.
 */

class ProjectExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.declaredClass == kotlin.Lazy::class.java
    }
}

class Vector2Serializer : JsonSerializer<Vector2>, JsonDeserializer<Vector2> {

    override fun serialize(src: Vector2, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            add(src.x)
            add(src.y)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vector2 {
        val array = json.asJsonArray
        return Vector2(array[0].asFloat, array[1].asFloat)
    }
}

class Vector3Serializer : JsonSerializer<Vector3>, JsonDeserializer<Vector3> {

    override fun serialize(src: Vector3, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            add(src.x)
            add(src.y)
            add(src.z)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vector3 {
        val array = json.asJsonArray
        return Vector3(array[0].asFloat, array[1].asFloat, array[2].asFloat)
    }

}

class Vector4Serializer : JsonSerializer<Vector4>, JsonDeserializer<Vector4> {

    override fun serialize(src: Vector4, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            add(src.x)
            add(src.y)
            add(src.z)
            add(src.w)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vector4 {
        val array = json.asJsonArray
        return Vector4(array[0].asFloat, array[1].asFloat, array[2].asFloat, array[3].asFloat)
    }

}

class QuaternionSerializer : JsonSerializer<Quaternion>, JsonDeserializer<Quaternion> {

    override fun serialize(src: Quaternion, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            add(src.x)
            add(src.y)
            add(src.z)
            add(src.w)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Quaternion {
        val array = json.asJsonArray
        return Quaternionf(array[0].asFloat, array[1].asFloat, array[2].asFloat, array[3].asFloat)
    }
}

class Matrix4Serializer : JsonSerializer<Matrix4>, JsonDeserializer<Matrix4> {

    override fun serialize(src: Matrix4, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonArray().apply {
            add(src.m00())
            add(src.m01())
            add(src.m02())
            add(src.m03())

            add(src.m10())
            add(src.m11())
            add(src.m12())
            add(src.m13())

            add(src.m20())
            add(src.m21())
            add(src.m22())
            add(src.m23())

            add(src.m30())
            add(src.m31())
            add(src.m32())
            add(src.m33())
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Matrix4 {
        val array = json.asJsonArray
        return Matrix4(
                array[0].asFloat, array[1].asFloat, array[2].asFloat, array[3].asFloat,
                array[4].asFloat, array[5].asFloat, array[6].asFloat, array[7].asFloat,
                array[8].asFloat, array[9].asFloat, array[10].asFloat, array[11].asFloat,
                array[12].asFloat, array[13].asFloat, array[14].asFloat, array[15].asFloat)
    }
}

class UUIDSerializer : JsonSerializer<UUID>, JsonDeserializer<UUID> {

    override fun serialize(src: UUID, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UUID {
        return UUID.fromString(json.asString)
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
        val str = json.asString
        val color = Color(str.toInt(16))
        return DColor(color.red / 255f, color.green / 255f, color.blue / 255f)
    }
}

interface BiSerializer<T> : JsonSerializer<T>, JsonDeserializer<T>

inline fun <reified T> serializerOf(): BiSerializer<T> {
    return object : BiSerializer<T> {

        override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
            return context.deserialize(json, T::class.java)
        }
    }
}

inline fun <T> Iterable<T>.toJsonArray(func: (T) -> JsonElement): JsonArray {
    return JsonArray().also {
        forEach { value ->
            it.add(func(value))
        }
    }
}

fun JsonArray.toVector2(): Vector2 = Vector2(this[0].asFloat, this[1].asFloat)
fun JsonArray.toVector3(): Vector3 = Vector3(this[0].asFloat, this[1].asFloat, this[2].asFloat)
fun JsonArray.toVector4(): Vector4 = Vector4(this[0].asFloat, this[1].asFloat, this[2].asFloat, this[3].asFloat)

val JsonElement.asVector2: Vector2 get() = this.asJsonArray.toVector2()
val JsonElement.asVector3: Vector3 get() = this.asJsonArray.toVector3()
val JsonElement.asVector4: Vector4 get() = this.asJsonArray.toVector4()

inline fun <reified T> JsonDeserializationContext.deserializeT(json: JsonElement): T {
    return deserialize(json, object : TypeToken<T>() {}.type)
}

inline fun <reified T> JsonSerializationContext.serializeT(obj: T): JsonElement {
    return serialize(obj, object : TypeToken<T>() {}.type)
}

inline fun <reified T> Gson.fromJson(json: String): T {
    return fromJson(json, object : TypeToken<T>() {}.type)
}