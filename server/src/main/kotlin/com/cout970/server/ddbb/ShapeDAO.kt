package com.cout970.server.ddbb

import com.cout970.server.rest.Defs
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Shape.*
import com.cout970.server.rest.Vector2
import com.cout970.server.rest.Vector3
import com.cout970.server.util.TerrainLoader
import com.cout970.server.util.TerrainLoader.ORIGIN
import com.cout970.server.util.areaOf
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Dims3d
import eu.printingin3d.javascad.models.Cube
import org.joml.Vector3f
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point
import kotlin.streams.toList
import org.postgis.Polygon as PGPolygon


object ShapeDAO {

    lateinit var buildings: List<ExtrudeSurface>
    lateinit var streets: List<ExtrudeSurface>
    lateinit var schools: List<ExtrudeSurface>
    lateinit var parks: List<ShapeAtSurface>
    lateinit var lightPoints: List<ShapeAtPoint>

    fun loadData() {
        val pairs = areaOf(0..0, 0..0).toList()

        buildings = pairs.parallelStream()
                .flatMap { pos -> ShapeDAO.getBuildings(pos).stream() }
                .toList()

        streets = pairs.parallelStream()
                .flatMap { pos -> ShapeDAO.getStreets(pos).stream() }
                .toList()

        lightPoints = pairs.parallelStream()
                .flatMap { pos -> ShapeDAO.getLightPoints(pos).stream() }
                .toList()

        schools = pairs.parallelStream()
                .flatMap { pos -> ShapeDAO.getSchools(pos).stream() }
                .toList()

        parks = pairs.parallelStream()
                .flatMap { pos -> ShapeDAO.getParks(pos).stream() }
                .toList()
    }

    private fun getBuildings(pos: Pair<Int, Int>): List<ExtrudeSurface> {
        var count = 0
        val sql = """
                SELECT geom, plantas
                FROM "edificación alturas", ${getAreaString(pos)} AS area
                WHERE ST_Within(geom, area);
                      """

        val buildings = DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val floors = it.getInt("plantas")

            val multiPolygon = geom.geometry as MultiPolygon
            val color = randomColor2() //randomColor()
            count++

            multiPolygon.polygons.map { poly ->
                Triple(poly.toPolygon().relativize(), floors, color)
            }

        }.flatten()

        println("Loaded $count geometries")

        return buildings.map { (polygon, floors, color) ->

            ExtrudeSurface(
                    surface = polygon,
                    height = floors * 3.5f,
                    rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                    scale = Vector3(1f),
                    projection = DefaultGroundProjection(0f),
                    material = Defs.Material(
                            ambientIntensity = 0.5f,
                            shininess = 0f,
                            diffuseColor = color,
                            emissiveColor = Defs.Color(0f, 0f, 0f),
                            specularColor = Defs.Color(1f, 1f, 1f),
                            transparency = 0f
                    )
            )
        }
    }

    private fun getSchools(pos: Pair<Int, Int>): List<ExtrudeSurface> {
        var count = 0
        val sql = """
                SELECT geom
                FROM "centros de enseñanza (polígono)", ${getAreaString(pos)} AS area
                WHERE ST_Within(geom, area);
                      """

        val buildings = DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry

            val multiPolygon = geom.geometry as MultiPolygon
            val color = randomColor4() //randomColor()
            count++

            multiPolygon.polygons.map { poly ->
                Triple(poly.toPolygon().relativize(), 3, color)
            }

        }.flatten()

        println("Loaded $count geometries")

        return buildings.map { (polygon, floors, color) ->

            ExtrudeSurface(
                    surface = polygon,
                    height = 1f,
                    rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                    scale = Vector3(1f),
                    projection = DefaultGroundProjection(0f),
                    material = Defs.Material(
                            ambientIntensity = 0.5f,
                            shininess = 0f,
                            diffuseColor = color,
                            emissiveColor = Defs.Color(0f, 0f, 0f),
                            specularColor = Defs.Color(1f, 1f, 1f),
                            transparency = 0f
                    )
            )
        }
    }

    private fun getParks(pos: Pair<Int, Int>): List<ShapeAtSurface> {
        var count = 0
        val sql = """
                SELECT geom
                FROM "parques (polígono)", ${getAreaString(pos)} AS area
                WHERE ST_Within(geom, area);
                      """

        val buildings = DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry

            val multiPolygon = geom.geometry as MultiPolygon
            count++

            multiPolygon.polygons.map { poly ->
                poly.toPolygon().relativize()
            }

        }.flatten()

        println("Loaded $count geometries")

        val geom = Cube(Dims3d(1.0, 10.0, 1.0)).toGeometry()
        val mat = Defs.Material(
                ambientIntensity = 0.5f,
                shininess = 0f,
                diffuseColor = Defs.Color(0f, 0.5f, 0f),
                emissiveColor = Defs.Color(0f, 0f, 0f),
                specularColor = Defs.Color(1f, 1f, 1f),
                transparency = 0f
        )
        val treeModel = Defs.Model(geom, mat)

        return buildings.map { polygon ->

            ShapeAtSurface(
                    model = treeModel,
                    surface = polygon,
                    rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                    scale = Vector3(1f),
                    projection = DefaultGroundProjection(0f),
                    resolution = 0.01f
            )
        }
    }

    private fun getStreets(pos: Pair<Int, Int>): List<ExtrudeSurface> {
        var count = 0
        val sql = """
                SELECT geom
                FROM calles, ${getAreaString(pos)} AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """

        val streets = DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry

            val multiPolygon = geom.geometry as MultiPolygon
            val color = randomColor3() //randomColor()
            count++

            multiPolygon.polygons.map { poly ->
                Pair(poly.toPolygon().relativize(), color)
            }
        }.flatten()

        println("Loaded $count geometries")

        return streets.map { (polygon, color) ->

            ExtrudeSurface(
                    surface = polygon,
                    height = 0.5f,
                    rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                    scale = Vector3(1f),
                    projection = DefaultGroundProjection(1f),
                    material = Defs.Material(
                            ambientIntensity = 0.5f,
                            shininess = 0f,
                            diffuseColor = color,
                            emissiveColor = Defs.Color(0f, 0f, 0f),
                            specularColor = Defs.Color(1f, 1f, 1f),
                            transparency = 0f
                    )
            )
        }
    }

    private fun getLightPoints(pos: Pair<Int, Int>): List<ShapeAtPoint> {
        var count = 0
        val sql = """
                SELECT geom
                FROM "puntos de luz", ${getAreaString(pos)} AS area
                WHERE municipio = '078' AND ST_Within(geom, area);
                      """


        val points = DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry

            val point = geom.geometry as Point
            count++

            Vector2(point.x.toFloat() - ORIGIN.x, point.y.toFloat() - ORIGIN.z)
        }

        println("Loaded $count geometries")

        val geom = Cube.fromCoordinates(
                Coords3d(0.4, 0.0, 0.4),
                Coords3d(0.6, 4.0, 0.6)
        ).addModel(Cube.fromCoordinates(
                Coords3d(0.0, 4.0, 0.0),
                Coords3d(1.0, 5.0, 1.0)
        )).toGeometry()

        val mat = Defs.Material(
                ambientIntensity = 0.5f,
                shininess = 0f,
                diffuseColor = Defs.Color(1f, 1f, 0.0f),
                emissiveColor = Defs.Color(0.1f, 0.1f, 0.1f),
                specularColor = Defs.Color(.1f, 1f, 1f),
                transparency = 0f
        )

        val model = Defs.Model(geom, mat)

        return points.map {
            ShapeAtPoint(
                    position = Vector3(it.x, 0f, it.y),
                    rotation = Defs.Rotation(0f, Vector3f(0f, 0f, 0f)),
                    scale = Vector3(1f),
                    projection = DefaultGroundProjection(0f),
                    model = model
            )
        }
    }

    private fun getAreaString(pos: Pair<Int, Int>): String {
        val minX = TerrainLoader.ORIGIN.x + (-2) * 1000
        val minY = TerrainLoader.ORIGIN.z + (-2) * 1000
        val maxX = TerrainLoader.ORIGIN.x + (2) * 1000
        val maxY = TerrainLoader.ORIGIN.z + (2) * 1000

        return "ST_GeomFromText('POLYGON(($minX $minY,$minX $maxY,$maxX $maxY,$maxX $minY,$minX $minY))')"
    }

    private fun PGPolygon.toPolygon(): Polygon {
        val points = getRing(0).points.map { Vector2(it.x.toFloat(), it.y.toFloat()) }
        return Polygon(points)
    }

    private fun Polygon.relativize(): Polygon {
        // .flip()
        return Polygon(points.map { Vector2(it.x - TerrainLoader.ORIGIN.x, -(it.y - TerrainLoader.ORIGIN.z)) })
//        return Polygon(points.map { Vector2(it.x, it.y) })
    }

    private fun randomColor(): Defs.Color {
        val c = java.awt.Color.getHSBColor(Math.random().toFloat() * 360f, 0.5f, 1f)
        return Defs.Color(c.red / 255f, c.green / 255f, c.blue / 255f)
    }

    private fun randomColor2(): Defs.Color {
        // 273.1f chosen by fair dice!
        val c = java.awt.Color.getHSBColor(273.1f / 360f, 0.5f, 1f)
        return Defs.Color(c.red / 255f, c.green / 255f, c.blue / 255f)
    }

    private fun randomColor3(): Defs.Color {
        val c = java.awt.Color.getHSBColor(63.1f / 360f, 0.5f, 1f)
        return Defs.Color(c.red / 255f, c.green / 255f, c.blue / 255f)
    }

    private fun randomColor4(): Defs.Color {
        val c = java.awt.Color.getHSBColor(240.1f / 360f, 0.5f, 1f)
        return Defs.Color(c.red / 255f, c.green / 255f, c.blue / 255f)
    }

//    fun getBuildingsIn(pos: Pair<Int, Int>): Defs.Geometry {
//        val models: MutableList<Defs.Geometry> = mutableListOf()
//        var size = 0
//
//        DDBBManager.useConnection {
//
//            val query1 = """
//                SELECT ST_AsX3D(result) as x3d
//                FROM (SELECT geom
//                      FROM "edificación alturas", ${getAreaString(pos)} AS area
//                      WHERE ST_Within(geom, area)) as building,
//                     st_tesselate(building.geom) as triangleMesh,
//                     st_extrude(triangleMesh, 0, 0, 0) as extruded,
//                     st_geometryn(extruded, 1) as result;
//                """
//
//            query(query1).forEach {
//                val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//                val document = documentBuilder.parse(it.getAsciiStream("x3d"))
//
//                val indexedFaceSet = document.childNodes.item(0)
//                val coordinates = indexedFaceSet.childNodes.item(0)
//
//                val indicesStr = indexedFaceSet.attributes.getNamedItem("coordIndex").nodeValue
//                val posStr = coordinates.attributes.getNamedItem("point").nodeValue
//
//                val indices = indicesStr.split(' ').filter { it != "-1" }.map { it.toInt() }
//                val coords = posStr.split(' ').map { it.toDouble() }
//
//                models += MeshBuilder.buildGeometry(indices, TerrainLoader.relativize(coords))
//                size++
//            }
//        }
//
//        val start = System.currentTimeMillis()
//
//        val result = models.fold(Defs.Geometry(emptyList())) { a, b -> a.merge(b) }
//
//        println("All models ($size) merged in ${System.currentTimeMillis() - start} ms")
//        return result
//    }
}
