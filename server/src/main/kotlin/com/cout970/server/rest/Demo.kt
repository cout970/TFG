package com.cout970.server.rest

import com.cout970.server.rest.Defs.Color
import com.cout970.server.rest.Defs.GroundProjection.DefaultGroundProjection
import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Material
import com.cout970.server.rest.Defs.Model
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Rule
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape.ShapeAtLine
import com.cout970.server.rest.Defs.Shape.ShapeAtPoint
import com.cout970.server.rest.Defs.ViewPoint
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.models.Cube
import org.joml.Vector3f


fun createDemoScene(): Scene {

    // Geometry generation
    val cubeGeom = Cube.fromCoordinates(
            Coords3d(0.0, 0.0, 0.0),
            Coords3d(10.0, 10.0, 10.0)
    ).toGeometry()

    // Model generation
    val cubeModel = Model(
            geometry = cubeGeom,
            material = Material(
                    ambientIntensity = 0.5f,
                    shininess = 0f,
                    diffuseColor = Color(1f, 0f, 0f),
                    emissiveColor = Color(0f, 1f, 0f),
                    specularColor = Color(0f, 0f, 1f),
                    transparency = 0f
            )
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

    val lightsLayer = Layer(
            name = "Lights",
            description = "This layer show the illumination of an area",
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

    val mainViewPoint = ViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            camera = Defs.CameraType.PERSPECTIVE
    )

    return Scene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(lightsLayer)
    )
}