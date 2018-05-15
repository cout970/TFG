package com.cout970.server.rest

import com.cout970.server.ddbb.ShapeDAO
import com.cout970.server.rest.Defs.CameraType.PERSPECTIVE
import com.cout970.server.rest.Defs.Color
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.GroundProjection.SnapProjection
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Material
import com.cout970.server.rest.Defs.Model
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Rule
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape.*
import com.cout970.server.rest.Defs.ViewPoint
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.TerrainLoader
import com.cout970.server.util.areaOf
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Polyhedron
import org.joml.Vector3f

lateinit var scene: Scene


fun bakeScene() {
    println("Baking scene...")
    Rest.cacheMap.clear()
    println("Baking terrain...")
    TerrainLoader.bakeTerrain()
    scene = SceneBaker.bake(createDemoScene())
    println("Scene baked")
}

fun createDemoScene(): Scene {

    println("Baking buildings...")
    val buildings = SceneBaker.bakeShapes(ShapeDAO.buildings)
    println("Baking streets...")
    val streets = SceneBaker.bakeShapes(ShapeDAO.streets)
    println("Baking lights...")
    val lights = SceneBaker.bakeShapes(ShapeDAO.lightPoints)
    println("Baking lights...")
    val schools = SceneBaker.bakeShapes(ShapeDAO.schools)
    println("Baking lights...")
    val parks = SceneBaker.bakeShapes(ShapeDAO.parks)
    println("Building scene...")

    // Geometry generation
    val s = 25.0
    val triangles = areaOf(-80..80, -80..80).toList().flatMap { (x, y) ->
        listOf(
                Triangle3d(Coords3d((x + 1) * s, 0.0, (y + 1) * s), Coords3d((x + 1) * s, 0.0, y * s), Coords3d(x * s, 0.0, y * s)),
                Triangle3d(Coords3d(x * s, 0.0, (y + 1) * s), Coords3d((x + 1) * s, 0.0, (y + 1) * s), Coords3d(x * s, 0.0, y * s))
        )
    }
    val terrain = Polyhedron(triangles).toGeometry()

    val cubeGeom2 = Cube.fromCoordinates(
            Coords3d(0.0, 0.0, 0.0),
            Coords3d(1.0, 1.0, 1.0)
    ).toGeometry()

    val cubeMaterial = Material(
            ambientIntensity = 0.0f,
            shininess = 0f,
            diffuseColor = Color(1f, 1f, 1f),
            emissiveColor = Color(0f, 0f, 0f),
            specularColor = Color(0f, 0f, 0f),
            transparency = 0.0f
    )

    // Model generation
    val terrainModel = Model(
            geometry = terrain,
            material = cubeMaterial.copy(diffuseColor = Color(0.0f, 1.0f, 0.0f))
    )

    val cubeModel2 = Model(
            geometry = cubeGeom2,
            material = cubeMaterial.copy(diffuseColor = Color(0.0f, 1.0f, 0.5f))
    )

    // Rest of the scene

    val cubeShapePoint = ShapeAtPoint(
            model = terrainModel,
            position = Vector3f(10f, 0f, 10f),
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            projection = SnapProjection(0f)
    )

    val cubeShapeLine = ShapeAtLine(
            model = cubeModel2,
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            lineStart = Vector3f(0f, 0f, 0f),
            lineEnd = Vector3f(1f, 0f, 1f),
            initialGap = 0f,
            gap = 25f,
            projection = DefaultGroundProjection(0f, true)
    )

    val cubeShapeSurface = ShapeAtSurface(
            model = terrainModel,
            surface = Polygon(listOf(
                    Vector2(0f, 0f),
                    Vector2(1000f, 0f),
                    Vector2(1000f, 1000f),
                    Vector2(0f, 1000f)
            )),
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            resolution = 0.001f,
            projection = SnapProjection(0f)
    )

    val cubeShapeExtrude = ExtrudeSurface(
            surface = Polygon(listOf(
                    Vector2(1000f, 0f),
                    Vector2(2000f, 0f),
                    Vector2(2000f, 1000f),
                    Vector2(1000f, 1000f)
            )),
            height = 20f,
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            material = cubeMaterial,
            projection = DefaultGroundProjection(0f, true)
    )

    val lightsLayer = Layer(
            name = "Debug Lights",
            description = "This layer shows a line of lights",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(cubeShapeLine)
            ), Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(lights)
            ))
    )

    val treesLayer = Layer(
            name = "Debug Tree area",
            description = "This layer shows a forest",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(cubeShapeSurface)
            ))
    )

    val buildingLayer = Layer(
            name = "Buildings",
            description = "This layer shows some buildings",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(buildings)
            ))
    )

    val streetsLayer = Layer(
            name = "Streets",
            description = "This layer shows the streets of the city",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(streets)
            ))
    )

    val TerrainLayer = Layer(
            name = "Terrain height",
            description = "This layer shows the ground level",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(SceneBaker.bakeShape(cubeShapePoint))
            ))
    )

    val schoolsLayer = Layer(
            name = "Schools and colleges",
            description = "Shows extra buildings",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(schools)
            ))
    )

    val parksLayer = Layer(
            name = "Parks",
            description = "Shows parks of the city",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(parks)
            ))
    )

    val mainViewPoint = ViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            camera = PERSPECTIVE
    )

    return Scene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(streetsLayer, TerrainLayer, buildingLayer, lightsLayer, parksLayer, schoolsLayer)
    )
}