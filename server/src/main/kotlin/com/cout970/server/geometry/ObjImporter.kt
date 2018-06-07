package com.cout970.server.geometry

import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.scene.DBufferGeometry
import com.cout970.server.scene.DGeometry
import com.cout970.server.scene.GeometryBuilder
import com.cout970.server.util.FloatArrayList
import com.cout970.server.util.info
import com.cout970.server.util.merge
import java.io.File

object ObjImporter {

    private const val separator = "/"
    private const val sVertex = "v "
    private const val sNormal = "vn "
    private const val sTexture = "vt "
    private const val sTexture2 = "vtc "
    private const val sFace = "f "
    private const val sGroup = "g "
    private const val sObject = "o "
    private const val sMaterial = "usemtl "
    private const val sLib = "mtllib "
    private const val sNewMaterial = "newmtl "
    private const val sMap_Ka = "map_Ka "
    private const val sMap_Kd = "map_Kd "
    private const val sComment = "#"
    private const val startIndex = 1 //index after label

    fun import(path: File, flipUvs: Boolean): DGeometry {
        val (data, groups, _) = parseFile(path, flipUvs)

        return groups
                .map { group -> group.toMesh(data) }
                .reduce { acc, dBufferGeometry -> acc.merge(dBufferGeometry) }
    }

    private fun ObjGroup.toMesh(data: MeshData): DBufferGeometry {
        val finalData = FloatArrayList()
        val pos = data.vertices

        quads.forEach {
            val p = it.vertexIndices
            when (p.size) {
                3 -> {
                    finalData.add(pos[p[0]].x)
                    finalData.add(pos[p[0]].y)
                    finalData.add(pos[p[0]].z)

                    finalData.add(pos[p[1]].x)
                    finalData.add(pos[p[1]].y)
                    finalData.add(pos[p[1]].z)

                    finalData.add(pos[p[2]].x)
                    finalData.add(pos[p[2]].y)
                    finalData.add(pos[p[2]].z)
                }
                4 -> {
                    finalData.add(pos[p[0]].x)
                    finalData.add(pos[p[0]].y)
                    finalData.add(pos[p[0]].z)

                    finalData.add(pos[p[1]].x)
                    finalData.add(pos[p[1]].y)
                    finalData.add(pos[p[1]].z)

                    finalData.add(pos[p[2]].x)
                    finalData.add(pos[p[2]].y)
                    finalData.add(pos[p[2]].z)

                    finalData.add(pos[p[0]].x)
                    finalData.add(pos[p[0]].y)
                    finalData.add(pos[p[0]].z)

                    finalData.add(pos[p[2]].x)
                    finalData.add(pos[p[2]].y)
                    finalData.add(pos[p[2]].z)

                    finalData.add(pos[p[3]].x)
                    finalData.add(pos[p[3]].y)
                    finalData.add(pos[p[3]].z)
                }
                else -> error("Invalid amount of vertices: ${it.vertexIndices.size}")
            }
        }

        return GeometryBuilder.build(finalData)
    }

    private fun parseFile(path: File, flipUvs: Boolean): Triple<MeshData, List<ObjGroup>, List<ObjMaterial>> {

        val input = path.inputStream()
        val vertices = mutableListOf<Vector3>()
        val texCoords = mutableListOf<Vector2>()
        val normals = mutableListOf<Vector3>()
        var hasTextures = false
        var hasNormals = false

        val noGroup = ObjGroup("noGroup", "noTexture", mutableListOf())
        val groups = mutableListOf<ObjGroup>()
        var quads = noGroup.quads
        var currentMaterial = "material"
        val materials = mutableListOf<ObjMaterial>()

        val lines = input.reader().readLines()

        for (line in lines) {
            val lineSpliced = line.split(" ")

            if (line.startsWith(sVertex)) { //vertex
                //reads a vertex
                vertices.add(Vector3(lineSpliced[startIndex].toFloat(),
                        lineSpliced[startIndex + 1].toFloat(),
                        lineSpliced[startIndex + 2].toFloat()))

            } else if (line.startsWith(sNormal)) { //normals

                hasNormals = true
                //read normals
                normals.add(Vector3(lineSpliced[startIndex].toFloat(),
                        lineSpliced[startIndex + 1].toFloat(),
                        lineSpliced[startIndex + 2].toFloat()))

            } else if (line.startsWith(sTexture) || line.startsWith(sTexture2)) { //textures

                hasTextures = true
                //reads a texture coords
                texCoords.add(Vector2(lineSpliced[startIndex].toFloat(),
                        if (flipUvs)
                            1 - lineSpliced[startIndex + 1].toFloat()
                        else
                            lineSpliced[startIndex + 1].toFloat()))

            } else if (line.startsWith(sFace)) { //faces
                val quad = ObjQuad()
                for (i in 1..4) {
                    val textVertex = if (i in lineSpliced.indices) lineSpliced[i] else lineSpliced[lineSpliced.size - 1]
                    val index = textVertex.split(separator)

                    quad.vertexIndices[i - 1] = index[0].toInt() - 1
                    if (hasTextures) {
                        quad.textureIndices[i - 1] = index[1].toInt() - 1
                        if (hasNormals) {
                            quad.normalIndices[i - 1] = index[2].toInt() - 1
                        }
                    } else {
                        if (hasNormals) {
                            quad.textureIndices[i - 1] = 0
                            quad.normalIndices[i - 1] = index[2].toInt() - 1
                        }
                    }
                }
                quads.add(quad)

            } else if (line.startsWith(sGroup) || line.startsWith(sObject)) {
                val newGroup = ObjGroup(lineSpliced[1], currentMaterial, mutableListOf())
                quads = newGroup.quads
                groups.add(newGroup)

            } else if (line.startsWith(sMaterial)) {
                currentMaterial = lineSpliced[1]
                noGroup.material = currentMaterial

            } else if (line.startsWith(sLib)) {
                try {
                    materials.addAll(parseMaterialLib(path.parentFile!!, lineSpliced[1]))
                } catch (e: Exception) {
                    info("Error reading the material library: ${e.message}")
                }
            } else if (!line.startsWith(sComment) && !line.isEmpty()) {
                if (lineSpliced[0] !in setOf("s")) {
                    info("Invalid line parsing OBJ ($path): '$line'")
                }
            }
        }
        if (noGroup.quads.isNotEmpty()) {
            groups.add(noGroup)
        }
        return Triple(MeshData(vertices, texCoords, normals), groups, materials)
    }

    private fun parseMaterialLib(resource: File, name: String): List<ObjMaterial> {
        val text = resource.inputStream().reader().readLines()

        val materialList = mutableListOf<ObjMaterial>()
        var material: ObjMaterial? = null
        for (line_ in text.asSequence()) {
            val line = line_.replace("\r", "")
            val lineSpliced = line.split(" ")

            if (line.startsWith(sNewMaterial)) {
                material?.let { materialList += it }
                material = ObjMaterial(lineSpliced[1])
            } else if (line.startsWith(sMap_Ka) || line.startsWith(sMap_Kd)) {
                try {
                    val subPath: String
                    if (lineSpliced[1].contains(":")) {
                        val slash = lineSpliced[1].substringAfter("/")
                        subPath = "textures/" + (if (slash.isEmpty()) lineSpliced[1].substringAfter(
                                ":") else slash) + ".png"
                    } else {
                        subPath = lineSpliced[1] + ".png"
                    }
                    material!!.map_Ka = resource.toPath().resolve(subPath).toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (!line.startsWith(sComment) && !line.isEmpty()) {
                // Ignoring line
            }
        }
        material?.let { materialList += it }
        return materialList
    }
}

private data class ObjMaterial(val name: String) {
    var map_Ka: String = ""
}

private class MeshData(
        val vertices: List<Vector3>,
        val texCoords: List<Vector2>,
        val normals: List<Vector3>
)

private class ObjGroup(
        val name: String,
        var material: String,
        val quads: MutableList<ObjQuad>
)

private class ObjQuad {
    var vertexIndices: IntArray
        internal set
    var textureIndices: IntArray
        internal set
    var normalIndices: IntArray
        internal set

    init {
        this.vertexIndices = IntArray(4)
        this.textureIndices = IntArray(4)
        this.normalIndices = IntArray(4)
    }
}