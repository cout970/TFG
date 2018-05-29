package com.cout970.server.util

import com.cout970.server.ddbb.DDBBManager
import com.cout970.server.glTF.GLTFBuilder
import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.glTF.gltfModel
import com.cout970.server.rest.*
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.collections.FloatArrayList
import eu.printingin3d.javascad.basic.Angle
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.LinearExtrude
import eu.printingin3d.javascad.models2d.Polygon
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.sqrt

object SceneBaker {

    fun bake(scene: DScene, buffer: String) = gltfModel {
        bufferName = buffer

        scene.layers.forEach { layer ->

            scene {
                name = layer.name

                layer.rules.forEachIndexed { index, rule ->

                    node {
                        name = "rule $index"

                        val models = rule.shapes.flatMap { getShapes(it).map { bakeShape(it) }.simplify() }

                        models.forEachIndexed { index, model ->

                            node {
                                name = "shape $index"
                                shapeMesh(this, model)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun GLTFBuilder.shapeMesh(node: GLTFBuilder.Node, model: Model) = node.apply {
        mesh {

            // TODO add material
            val geom = model.geometry

            primitive {

                val data: FloatArray = geom.attributes[0].data
                val vertices = data.toList().windowed(3, 3).map { Vector3(it[0], it[1], it[2]) }

                attributes[POSITION] = buffer(FLOAT, vertices)
            }
        }
    }

    // Shape factories

    fun getShapes(source: DShapeSource): List<DShape> = when (source) {
        is DInlineShapeSource -> listOf(source.shape)
        is DExtrudedShapeSource -> getShapes(source)
        is DShapeAtPointSource -> getShapes(source)
    }

    fun getShapes(source: DExtrudedShapeSource): List<DShape> {
        val src = source.polygonsSource
        val geoms = DDBBManager.loadExtrudedPolygons(src.geomField, src.heightField, src.tableName, src.heightScale, src.area.toSQL())

        return geoms.flatMap { epg ->
            epg.polygons.map { poly ->
                ExtrudeSurface(
                        surface = poly,
                        height = epg.height,
                        material = source.material,
                        projection = source.projection
                )
            }
        }
    }

    fun getShapes(source: DShapeAtPointSource): List<DShape> {
        val geom = getGeometry(source.geometrySource)
        val src = source.points
        val points: List<Vector2> = DDBBManager.loadPoints(src.geomField, src.geomField, src.area.toSQL())

        return points.map {
            ShapeAtPoint(
                    geometry = geom,
                    point = it,
                    projection = source.projection,
                    material = source.material
            )
        }
    }

    // shape baking

    fun bakeShape(shape: DShape): Model {
        try {
            return when (shape) {
                is ShapeAtPoint -> bakeShapeAtPoint(shape)
                is ShapeAtLine -> bakeShapeAtLine(shape)
                is ShapeAtSurface -> bakeShapeAtSurface(shape)
                is ExtrudeSurface -> bakeExtrudeShape(shape)
            }
        } catch (e: Exception) {

            return Model(
                    material = DMaterial(
                            ambientIntensity = 0.5f,
                            diffuseColor = DColor(1f, 1f, 1f),
                            emissiveColor = DColor(0f, 0f, 0f),
                            shininess = 1f,
                            specularColor = DColor(0.5f, 0.5f, 0.5f),
                            transparency = 0f
                    ),
                    geometry = Cube(10.0).toGeometry().bake()
            )
        }
    }

    private fun bakeShapeAtPoint(shape: ShapeAtPoint): Model {
        val translation = DTransformGeometry(
                translation = Vector3(shape.point.x, 0f, shape.point.y),
                source = shape.geometry
        )
        val newGeometry = translation.bake().project(shape.projection)

        return Model(newGeometry, shape.material)
    }

    private fun bakeShapeAtLine(shape: ShapeAtLine): Model {
        val geometry = shape.geometry.bake()

        val direction = (shape.lineEnd - shape.lineStart).normalize()
        val start = shape.lineStart + (direction * shape.initialGap)

        val line = shape.lineEnd - start
        val numPoints = sqrt(line.x * line.x + line.z * line.z).toInt()

        val geometries = mutableListOf<DBufferGeometry>()

        repeat(numPoints) { i ->
            val point2d = start + (direction * shape.gap * i.toFloat())

            val translation = DTransformGeometry(
                    translation = point2d,
                    source = geometry
            )
            geometries += translation.bake().project(shape.projection)
        }

        val newGeometry = geometries.reduce { acc, geom -> acc.merge(geom) }
        return Model(newGeometry, shape.material)
    }

    private fun bakeShapeAtSurface(shape: ShapeAtSurface): Model {
        val geometry = shape.geometry.bake()

        val locations = mutableListOf<Vector2>()
        val geometries = mutableListOf<DBufferGeometry>()

        val triangles = Triangulator.triangulate(shape.surface)

        triangles.forEach { (point1, point2, point3) ->

            // http://mathworld.wolfram.com/TrianglePointPicking.html
            val v0 = Vector2(0f)
            val v1 = point2 - point1
            val v2 = point3 - point1

            val area = getArea(v0, v1, v2)
            val elements = (area * shape.resolution * 2).toInt()

            repeat(elements) {
                val a1 = Math.random().toFloat()
                val a2 = Math.random().toFloat()
                val newPoint = v1 * a1 + v2 * a2

                if (inTriangle(newPoint, v0, v1, v2)) {
                    locations += (newPoint + point1)
                }
            }
        }

        locations.forEach { point2d ->
            val translation = DTransformGeometry(
                    translation = Vector3(point2d.x, 0f, point2d.y),
                    source = geometry
            )
            geometries += translation.bake().project(shape.projection)
        }

        val newGeometry = geometries.reduce { acc, geom -> acc.merge(geom) }
        return Model(newGeometry, shape.material)
    }

    private fun bakeExtrudeShape(shape: ExtrudeSurface): Model {
        val surface = shape.surface
        require(surface.points.size >= 3) {
            "Polygon must have at least 3 points, it had ${surface.points.size} instead"
        }

        val newGeometry = extrude(surface, shape.height).bake().project(projection = shape.projection)

        return Model(newGeometry, shape.material)
    }

    // Geometry accessors

    private fun getGeometry(source: DGeometrySource): DGeometry = when (source) {
        is DPolygonsSource -> getGeometry(source)
        is DExtrudedPolygonsSource -> getGeometry(source)
        is DInlineSource -> source.geometry
    }

    private fun getGeometry(src: DPolygonsSource): DGeometry {
        val polys = DDBBManager.loadPolygons(src.geomField, src.tableName, src.area.toSQL())
        return polys.flatMap { it.polygons }.toGeometry()
    }

    private fun getGeometry(src: DExtrudedPolygonsSource): DGeometry {
        val geoms = DDBBManager.loadExtrudedPolygons(src.geomField, src.heightField, src.tableName, src.heightScale, src.area.toSQL())
        return geoms.flatMap { it.polygons }.toGeometry()
    }

    // Geometry baking

    private fun DGeometry.bake(): DBufferGeometry = when (this) {
        is DBufferGeometry -> this
        is DTransformGeometry -> this.source.bake().transform(this.translation, this.rotation, this.scale)
    }

    private fun DBufferGeometry.transform(translation: Vector3, rotation: DRotation, scale: Vector3): DBufferGeometry {
        val matrix = Matrix4f().apply {
            translate(translation)
            rotate(rotation.angle, rotation.axis)
            scale(scale)
        }

        val newAttributes = attributes.map { attr ->
            if (attr.attributeName != "position") return@map attr

            val data = expandTriangles(attr.data)

            val newData = FloatArray(data.size)
            val input = Vector4f(0f, 0f, 0f, 1f)
            val output = Vector4f(0f, 0f, 0f, 1f)

            repeat(data.size / 3) { i ->
                input.x = data[i * 3]
                input.y = data[i * 3 + 1]
                input.z = data[i * 3 + 2]

                matrix.transform(input, output)

                newData[i * 3] = output.x
                newData[i * 3 + 1] = output.y
                newData[i * 3 + 2] = output.z
            }

            attr.copy(data = newData)
        }

        return copy(attributes = newAttributes)
    }

    private fun DBufferGeometry.project(projection: DGroundProjection): DBufferGeometry {
        val newAttributes = attributes.map { attr ->
            if (attr.attributeName != "position") return@map attr

            attr.copy(data = project(projection, attr.data))
        }

        return copy(attributes = newAttributes)
    }

    // utilities

    private fun extrude(polygon: DPolygon, height: Float): DGeometry {
        val coords = polygon.points.map { Coords2d(it.x.toDouble(), it.y.toDouble()) }
        val model = LinearExtrude(Polygon(coords), height.toDouble(), Angle.ZERO, 1.0)

        val polygons = model.toCSG(FacetGenerationContext.DEFAULT).polygons

        val yOffset = height / 2
        val triangles = polygons.flatMap { Triangulator.triangulate(it) }.map { tri ->
            val (a, b, c) = tri.points
            Triangle3d(
                    Coords3d(a.x, a.z + yOffset, a.y),
                    Coords3d(b.x, b.z + yOffset, b.y),
                    Coords3d(c.x, c.z + yOffset, c.y)
            )
        }

        return triangles.toGeometry()
    }

    private fun project(projection: DGroundProjection, points: FloatArray): FloatArray {

        when (projection) {
            is DefaultGroundProjection -> {
                var min = Float.MAX_VALUE
                var max = Float.MIN_VALUE

                points.indices.windowed(3, 3).forEach { i ->
                    val xPos = points[i[0]] // + TerrainLoader.ORIGIN.x
                    val zPos = points[i[2]] // + TerrainLoader.ORIGIN.z

                    val h = TerrainLoader.getHeight(xPos, zPos)
                    min = Math.min(min, h)
                    max = Math.max(max, h)
                }

                if (projection.top) {
                    points.indices.windowed(3, 3).forEach { i ->
                        points[i[1]] += max
                    }

                } else {
                    points.indices.windowed(3, 3).forEach { i ->
                        points[i[1]] += min
                    }
                }
            }
            is SnapProjection -> {
                points.indices.windowed(3, 3).forEach { i ->
                    val xPos = points[i[0]] //+ TerrainLoader.ORIGIN.x
                    val zPos = points[i[2]] //+ TerrainLoader.ORIGIN.z

                    points[i[1]] += TerrainLoader.getHeight(xPos, zPos)
                }
            }
            is BridgeGroundProjection -> TODO("")
        }

        return points
    }

    //https://stackoverflow.com/questions/2049582/how-to-determine-if-a-point-is-in-a-2d-triangle
    private fun inTriangle(p: Vector2, p0: Vector2, p1: Vector2, p2: Vector2): Boolean {
        val area = getArea(p0, p1, p2)
        val s = 1 / (2 * area) * (p0.y * p2.x - p0.x * p2.y + (p2.y - p0.y) * p.x + (p0.x - p2.x) * p.y)
        val t = 1 / (2 * area) * (p0.x * p1.y - p0.y * p1.x + (p0.y - p1.y) * p.x + (p1.x - p0.x) * p.y)
        return s > 0 && t > 0 && t + s <= 1
    }

    private fun getArea(p0: Vector2, p1: Vector2, p2: Vector2): Float {
        return 0.5f * (-p1.y * p2.x + p0.y * (-p1.x + p2.x) + p0.x * (p1.y - p2.y) + p1.x * p2.y)
    }

    private fun expandTriangles(data: FloatArray): FloatArray {
        val list = FloatArrayList()
        val p0 = Vector3()
        val p1 = Vector3()
        val p2 = Vector3()

        repeat(data.size / 9) { i ->
            p0.x = data[i * 9]
            p0.y = data[i * 9 + 1]
            p0.z = data[i * 9 + 2]

            p1.x = data[i * 9 + 3]
            p1.y = data[i * 9 + 4]
            p1.z = data[i * 9 + 5]

            p2.x = data[i * 9 + 6]
            p2.y = data[i * 9 + 7]
            p2.z = data[i * 9 + 8]

            val p10 = p0 - p1
            val p20 = p0 - p2
            val p21 = p1 - p2

            if (p10.length() > 50 || p20.length() > 50 || p21.length() > 50) {

                val a = p10 * 0.5f + p1
                val b = p20 * 0.5f + p2
                val c = p21 * 0.5f + p2

                // left-down
                list.add(p0)
                list.add(a + Vector3(0f, 100f, 0f))
                list.add(b)

                //center
                list.add(a)
                list.add(c + Vector3(0f, 100f, 0f))
                list.add(b)

                // right-down
                list.add(b)
                list.add(c + Vector3(0f, 100f, 0f))
                list.add(p2)

                // top
                list.add(a)
                list.add(p1 + Vector3(0f, 100f, 0f))
                list.add(c)

            } else {
                list.add(p0)
                list.add(p1)
                list.add(p2)
            }
        }
        return list.toFloatArray()
    }

    private fun FloatArrayList.add(i: Vector3) {
        add(i.x)
        add(i.y)
        add(i.z)
    }

    private fun List<Model>.simplify(): List<Model> {
        val map = groupBy { it.material }

        return map.map {
            val geom = it.value
                    .parallelStream()
                    .map { it.geometry }
                    .reduce { acc, geometry -> acc.merge(geometry) }

            Model(geom.get(), it.key)
        }
    }
}