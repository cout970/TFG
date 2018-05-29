package com.cout970.server.glTF

import com.google.gson.*
import java.io.File
import java.lang.reflect.Type
import java.nio.ByteBuffer
import java.nio.ByteOrder


// Example
fun main(args: Array<String>) {
    val (header, buffer) = testExporter()
    val file = File("debug.gltf")

    file.writeText(GLTF_GSON.toJson(header))

    File(header.buffers[0].uri).writeBytes(buffer)
}

fun testExporter() = gltfModel {

    bufferName = "debug.bin"

    useExtensions("myExtension")
    requireExtensions("myExtrensionLoader")

    asset {
        copyright = "GNU GPL v3"
    }

    scene {
        node {
            name = "root"

            transformation {
                translation = Vector3()
                scale = Vector3(1f)
            }

            node {
                name = "child1"
                cubeMesh(this)
            }
        }
    }
}

private fun GLTFBuilder.cubeMesh(node: GLTFBuilder.Node) = node.apply {
    mesh {
        name = "Cube"

        primitive {
            mode = TRIANGLES

            attributes[POSITION] = buffer(FLOAT, listOf(
                    Vector3(-1.0f, 1.0f, 1.0f),
                    Vector3(1.0f, 1.0f, 1.0f),
                    Vector3(-1.0f, -1.0f, 1.0f),
                    Vector3(1.0f, -1.0f, 1.0f),
                    Vector3(-1.0f, 1.0f, -1.0f),
                    Vector3(1.0f, 1.0f, -1.0f),
                    Vector3(-1.0f, -1.0f, -1.0f),
                    Vector3(1.0f, -1.0f, -1.0f)
            ))

            indices = buffer(UNSIGNED_INT, listOf(
                    0, 1, 2, // 0
                    1, 3, 2,
                    4, 6, 5, // 2
                    5, 6, 7,
                    0, 2, 4, // 4
                    4, 2, 6,
                    1, 5, 3, // 6
                    5, 7, 3,
                    0, 4, 1, // 8
                    4, 5, 1,
                    2, 3, 6, // 10
                    6, 3, 7
            ))
        }
    }
}

// Serializer
val GLTF_GSON = GsonBuilder()
        .registerTypeAdapter(Vector4::class.java, Vector4Serializer())
        .registerTypeAdapter(Vector3::class.java, Vector3Serializer())
        .registerTypeAdapter(Vector2::class.java, Vector2Serializer())
        .registerTypeAdapter(Quaternion::class.java, QuaternionSerializer())
        .registerTypeAdapter(Matrix4::class.java, Matrix4Serializer())
        .registerTypeAdapter(GltfAccessor::class.java, AccessorSerializer)
        .registerTypeAdapter(GltfBufferView::class.java, BufferViewSerializer)
        .registerTypeAdapter(List::class.java, EmptyListAdapter)
        .registerTypeAdapter(Map::class.java, EmptyMapAdapter)
        .setPrettyPrinting()
        .create()


private object EmptyListAdapter : JsonSerializer<List<*>> {

    override fun serialize(src: List<*>?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement? {
        if (src == null || src.isEmpty())
            return null

        return context.serialize(src)
    }
}

private object EmptyMapAdapter : JsonSerializer<Map<*, *>> {

    override fun serialize(src: Map<*, *>?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement? {
        if (src == null || src.isEmpty())
            return null

        return context.serialize(src)
    }
}

private object AccessorSerializer : JsonSerializer<GltfAccessor> {

    override fun serialize(src: GltfAccessor, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {

            addProperty("bufferView", src.bufferView)

            if (src.byteOffset != null && src.byteOffset != 0)
                addProperty("byteOffset", src.byteOffset)

            addProperty("componentType", src.componentType)

            if (src.normalized != null && src.normalized)
                addProperty("normalized", src.normalized)

            addProperty("count", src.count)

            add("type", context.serialize(src.type))

            if (src.max.isNotEmpty())
                add("max", context.serialize(src.max))

            if (src.min.isNotEmpty())
                add("min", context.serialize(src.min))

            if (src.sparse != null)
                add("sparse", context.serialize(src.sparse))

            if (src.name != null)
                addProperty("name", src.name)
        }
    }
}

private object BufferViewSerializer : JsonSerializer<GltfBufferView> {

    override fun serialize(src: GltfBufferView, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {

            addProperty("buffer", src.buffer)

            if (src.byteOffset != null && src.byteOffset != 0)
                addProperty("byteOffset", src.byteOffset)

            addProperty("byteLength", src.byteLength)

            if (src.byteStride != null)
                addProperty("byteStride", src.byteStride)

            if (src.target != null)
                addProperty("target", src.target)

            if (src.name != null)
                addProperty("name", src.name)
        }
    }
}

fun gltfModel(func: GLTFBuilder.() -> Unit): Pair<GltfFile, ByteArray> {
    val builder = GLTFBuilder()
    builder.func()
    return builder.build()
}

val GltfFile.bufferName: String get() = buffers[0].uri ?: ""

class GLTFBuilder {
    private val extensionsUsed = mutableListOf<String>()
    private val extensionsRequired = mutableListOf<String>()
    private val asset = Asset()

    private val scenes = mutableListOf<Scene>()
    private val bakedNodes = mutableListOf<GltfNode>()
    private val bakedMeshes = mutableListOf<GltfMesh>()
    private var buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
    private val bakedBufferViews = mutableListOf<GltfBufferView>()
    private val bakedAccessors = mutableListOf<GltfAccessor>()
    var bufferName = "model.bin"

    fun useExtensions(vararg extensionsUsed: String) {
        this.extensionsUsed.addAll(extensionsUsed)
    }

    fun requireExtensions(vararg extensionsRequired: String) {
        this.extensionsRequired.addAll(extensionsRequired)
    }

    fun build(): Pair<GltfFile, ByteArray> {
        val scenes = scenes.build()
        val binary = buffer.toArray()

        return GltfFile(
                asset = asset.build(),
                nodes = bakedNodes,
                meshes = bakedMeshes,
                bufferViews = bakedBufferViews,
                accessors = bakedAccessors,
                scene = 0,
                scenes = scenes,
                buffers = listOf(GltfBuffer(uri = bufferName, byteLength = binary.size))
        ) to binary
    }

    data class Asset(
            var copyright: String? = null
    )

    fun asset(func: Asset.() -> Unit) {
        this.asset.func()
    }

    fun Asset.build(): JsObject {
        val map = mutableMapOf(
                "generator" to "TFG Gltf Exporter",
                "version" to "2.0"
                // minVersion,
        )

        copyright?.let { map["copyright"] = it }
        return map
    }

    data class Scene(
            val nodes: MutableList<Node> = mutableListOf(),
            var name: String? = null
    )

    fun scene(func: Scene.() -> Unit) {
        scenes.add(Scene().apply(func))
    }

    @JvmName("buildScenes")
    private fun List<Scene>.build(): List<GltfScene> {
        return map { it.build() }
    }

    fun Scene.build(): GltfScene {
        val indices = nodes.map {
            it.build()
            bakedNodes.size - 1
        }

        return GltfScene(
                nodes = indices,
                name = name
        )
    }

    data class Node(
            var transformation: Transformation? = null,
            var name: String? = null,
            var children: MutableList<Node>? = null,
            var mesh: Mesh? = null

//            val camera: Int? = null,                 // The index of the camera referenced by this node.
//            val skin: Int? = null,                 // The index of the skin referenced by this node.

//            val weights: List<Double> = emptyList(),          // The weights of the instantiated Morph Target. Number of elements must match number of Morph Targets of used mesh.
    )

    sealed class Transformation {
        data class Matrix(var matrix: Matrix4) : Transformation()
        data class TRS(
                var translation: Vector3? = null,
                var rotation: Quaternion? = null,
                var scale: Vector3? = null
        ) : Transformation()
    }

    fun Scene.node(func: Node.() -> Unit) {
        nodes.add(Node().apply(func))
    }

    fun Node.node(func: Node.() -> Unit) {
        val list = children ?: mutableListOf()
        list.add(Node().apply(func))
        children = list
    }

    fun Node.transformation(mat: Matrix4) {
        transformation = Transformation.Matrix(mat)
    }

    fun Node.transformation(func: Transformation.TRS.() -> Unit) {
        transformation = Transformation.TRS().apply(func)
    }

    @JvmName("buildNodes")
    private fun List<Node>.build(): List<GltfNode> {
        bakedNodes.clear()
        forEach { it.build() }
        return bakedNodes
    }

    fun Node.build(): GltfNode {
        val bakedChildren = children?.map { it.build() }
        val t = transformation
        val m = mesh?.build()

        val node = GltfNode(
                name = name,
                matrix = (t as? Transformation.Matrix)?.matrix,
                translation = (t as? Transformation.TRS)?.translation,
                rotation = (t as? Transformation.TRS)?.rotation,
                scale = (t as? Transformation.TRS)?.scale,
                children = bakedChildren?.map { bakedNodes.indexOf(it) } ?: emptyList(),
                mesh = m?.let { bakedMeshes.indexOf(it) }
        )

        bakedNodes.add(node)
        return node
    }

    data class Mesh(
            var name: String? = null,
            var primitives: MutableList<Primitive> = mutableListOf(),
            var weights: MutableList<Double> = mutableListOf()
    )

    fun Node.mesh(func: Mesh.() -> Unit) {
        mesh = Mesh().apply(func)
    }

    fun Mesh.build(): GltfMesh {
        val mesh = GltfMesh(primitives.map { it.build() }, weights, name)
        bakedMeshes.add(mesh)
        return mesh
    }

    data class Primitive(
            val attributes: MutableMap<String, UnpackedBuffer> = mutableMapOf(),
            var indices: UnpackedBuffer? = null,
//            val material: Int? = null,
            var mode: GltfMode = GltfMode.TRIANGLES,
            val targets: MutableMap<String, Int> = mutableMapOf()
    ) {

        // this avoid having to import ComponentType.*
        inline val BYTE: GltfComponentType get() = GltfComponentType.BYTE
        inline val UNSIGNED_BYTE: GltfComponentType get() = GltfComponentType.UNSIGNED_BYTE
        inline val SHORT: GltfComponentType get() = GltfComponentType.SHORT
        inline val UNSIGNED_SHORT: GltfComponentType get() = GltfComponentType.UNSIGNED_SHORT
        inline val UNSIGNED_INT: GltfComponentType get() = GltfComponentType.UNSIGNED_INT
        inline val FLOAT: GltfComponentType get() = GltfComponentType.FLOAT

        // this avoid having to import Attribute.*
        inline val POSITION: String get() = GltfAttribute.POSITION.name
        inline val NORMAL: String get() = GltfAttribute.NORMAL.name
        inline val TANGEN: String get() = GltfAttribute.TANGENT.name
        inline val TEXCOORD_0: String get() = GltfAttribute.TEXCOORD_0.name
        inline val TEXCOORD_1: String get() = GltfAttribute.TEXCOORD_1.name
        inline val COLOR_0: String get() = GltfAttribute.COLOR_0.name
        inline val JOINTS_0: String get() = GltfAttribute.JOINTS_0.name
        inline val WEIGHTS_0: String get() = GltfAttribute.WEIGHTS_0.name

        // this avoid having to import GLMode.*
        inline val POINTS: GltfMode get() = GltfMode.POINTS
        inline val LINES: GltfMode get() = GltfMode.LINES
        inline val LINE_LOOP: GltfMode get() = GltfMode.LINE_LOOP
        inline val LINE_STRIP: GltfMode get() = GltfMode.LINE_STRIP
        inline val TRIANGLES: GltfMode get() = GltfMode.TRIANGLES
        inline val TRIANGLE_STRIP: GltfMode get() = GltfMode.TRIANGLE_STRIP
        inline val TRIANGLE_FAN: GltfMode get() = GltfMode.TRIANGLE_FAN
        inline val QUADS: GltfMode get() = GltfMode.QUADS
        inline val QUAD_STRIP: GltfMode get() = GltfMode.QUAD_STRIP
        inline val POLYGON: GltfMode get() = GltfMode.POLYGON
    }

    fun Mesh.primitive(func: Primitive.() -> Unit) {
        primitives.add(Primitive().apply(func))
    }

    inline fun <reified T> Primitive.buffer(type: GltfComponentType, data: List<T>, indices: Boolean = false): UnpackedBuffer {
        val container: GltfType = when {
            Number::class.java.isAssignableFrom(T::class.java) -> GltfType.SCALAR
            Vector2::class.java.isAssignableFrom(T::class.java) -> GltfType.VEC2
            Vector3::class.java.isAssignableFrom(T::class.java) -> GltfType.VEC3
            Vector4::class.java.isAssignableFrom(T::class.java) -> GltfType.VEC4
//            IMatrix2::class.java.isAssignableFrom(T::class.java) -> GltfType.MAT2
            Matrix3::class.java.isAssignableFrom(T::class.java) -> GltfType.MAT3
            Matrix4::class.java.isAssignableFrom(T::class.java) -> GltfType.MAT4
            else -> error("Invalid buffer type")
        }
        return UnpackedBuffer(container, type, data, indices)
    }

    fun Primitive.build(): GltfPrimitive {
        return GltfPrimitive(
                attributes = attributes.mapValues { it.value.build() },
                indices = indices?.build(),
                mode = mode.code,
                targets = targets
        )
    }

    data class UnpackedBuffer(
            val containerType: GltfType,
            val elementType: GltfComponentType,
            val data: List<*>,
            val indices: Boolean
    )

    @Suppress("UNCHECKED_CAST")
    fun UnpackedBuffer.build(): Int {

        val size = elementType.size * containerType.numComponents * data.size
        val index = bakedBufferViews.size
        val view = GltfBufferView(
                buffer = 0,
                name = null,
                byteLength = size,
                byteOffset = buffer.position(),
                byteStride = null,
                target = if(indices) 34963 else 34962
        )
        val accessor = GltfAccessor(
                bufferView = index,
                byteOffset = 0,
                componentType = elementType.id,
                normalized = false,
                count = data.size,
                type = containerType,
                name = null
        )

        val put = { n: Number ->

            if (buffer.capacity() < buffer.position() + 16 * 4) {
                buffer = buffer.expand(buffer.capacity() * 2)
            }

            when (elementType) {
                GltfComponentType.BYTE, GltfComponentType.UNSIGNED_BYTE -> buffer.put(n.toByte())
                GltfComponentType.SHORT, GltfComponentType.UNSIGNED_SHORT -> buffer.putShort(n.toShort())
                GltfComponentType.UNSIGNED_INT -> buffer.putInt(n.toInt())
                GltfComponentType.FLOAT -> buffer.putFloat(n.toFloat())
            }
            Unit
        }

        when (containerType) {
            GltfType.SCALAR -> (data as List<Number>).forEach(put)
            GltfType.VEC2 -> (data as List<Vector2>).forEach { put(it.x); put(it.y) }
            GltfType.VEC3 -> (data as List<Vector3>).forEach { put(it.x); put(it.y); put(it.z) }
            GltfType.VEC4 -> (data as List<Vector4>).forEach { put(it.x); put(it.y); put(it.z); put(it.w) }
            GltfType.MAT2, GltfType.MAT3, GltfType.MAT4 -> error("Matrix storage not supported")
        }

        bakedBufferViews.add(view)
        bakedAccessors.add(accessor)
        return index
    }

    fun ByteBuffer.toArray(): ByteArray {
        val array = ByteArray(position())
        flip()
        repeat(array.size) {
            array[it] = get()
        }
        return array
    }

    fun ByteBuffer.expand(newSize: Int): ByteBuffer {
        this.flip()
        return ByteBuffer.allocate(newSize).put(this).order(ByteOrder.LITTLE_ENDIAN)
    }

//    val animations: List<Animation> = emptyList(),  // An array of keyframe animations.
//    val cameras: List<Camera> = emptyList(),  // An array of cameras. A camera defines a projection matrix.
//    val images: List<Image> = emptyList(),  // An array of images. An image defines data used to create a texture.
//    val materials: List<Material> = emptyList(),  // An array of materials. A material defines the appearance of a primitive.
//    val samplers: List<Sampler> = emptyList(),  // An array of samplers. A sampler contains properties for texture filtering and wrapping modes.
//    val scene: Int? = null,         // The index of the default scene.
//    val scenes: List<Scene> = emptyList(),  // An array of scenes.
//    val skins: List<Skin> = emptyList(),  // An array of skins. A skin is defined by joints and matrices.
//    val textures: List<Texture> = emptyList(),  // An array of textures.
}

