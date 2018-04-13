package com.cout970.server.rest

import com.cout970.server.ddbb.ShapeDAO
import com.cout970.server.rest.Defs.CameraType.PERSPECTIVE
import com.cout970.server.rest.Defs.Color
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Material
import com.cout970.server.rest.Defs.Model
import com.cout970.server.rest.Defs.Polygon
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Rule
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape
import com.cout970.server.rest.Defs.Shape.*
import com.cout970.server.rest.Defs.ViewPoint
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.merge
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.models.Cube
import org.joml.Vector3f
import java.util.*

lateinit var buildings: Shape
lateinit var scene: Scene

fun loadBuildings() {
    val pairs = (-5..0).flatMap { x -> (-5..0).map { y -> x to y } }

    buildings = pairs.parallelStream()
            .flatMap { pos -> ShapeDAO.getBuildings(pos).stream() }
            .map {
                try {
                    Optional.of(SceneBaker.bakeShape(it))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Optional.empty<BakedShape>()
                }
            }
            .filter { it.isPresent }
            .map { it.get() }
            .reduce { a: BakedShape, b: BakedShape -> a.merge(b) }
            .get()
}

fun bakeScene() {
    scene = SceneBaker.bake(createDemoScene())
}

fun createDemoScene(): Scene {

    // Geometry generation
    val cubeGeom = Cube.fromCoordinates(
            Coords3d(0.0, 0.0, 0.0),
            Coords3d(10.0, 10.0, 10.0)
    ).toGeometry()

    val cubeMaterial = Material(
            ambientIntensity = 0.5f,
            shininess = 0f,
            diffuseColor = Color(1f, 0f, 0f),
            emissiveColor = Color(0f, 1f, 0f),
            specularColor = Color(0f, 0f, 1f),
            transparency = 0f
    )

    // Model generation
    val cubeModel = Model(
            geometry = cubeGeom,
            material = cubeMaterial
    )

    // Rest of the scene

    val cubeShapePoint = ShapeAtPoint(
            model = cubeModel,
            position = Vector3f(10f, 0f, 10f),
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f)
    )

    val cubeShapeLine = ShapeAtLine(
            model = cubeModel,
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            lineStart = Vector3f(-10f, 0f, 0f),
            lineEnd = Vector3f(-10f, 0f, 100f),
            initialGap = 0f,
            gap = 25f,
            projection = DefaultGroundProjection(0f)
    )

    val cubeShapeSurface = ShapeAtSurface(
            model = cubeModel,
            surface = Polygon(listOf(
                    Vector2(0f, 0f),
                    Vector2(1000f, 0f),
                    Vector2(1000f, 1000f),
                    Vector2(0f, 1000f)
            )),
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f),
            resolution = 0.001f,
            projection = DefaultGroundProjection(0f)
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
            projection = DefaultGroundProjection(0f)
    )

    val lightsLayer = Layer(
            name = "Lights",
            description = "This layer shows a line of lights",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(cubeShapePoint)
            ), Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(cubeShapeLine)
            ))
    )

    val treesLayer = Layer(
            name = "Tree area",
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

    val mainViewPoint = ViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            camera = PERSPECTIVE
    )

    return Scene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(lightsLayer, treesLayer, buildingLayer)
    )
}