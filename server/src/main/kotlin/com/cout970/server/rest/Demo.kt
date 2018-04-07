package com.cout970.server.rest

import com.cout970.server.rest.Defs.Layer
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Rule
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.Shape.ShapeAtPoint
import com.cout970.server.rest.Defs.ViewPoint
import org.joml.Vector3f


fun createDemoScene(): Scene {


    val cube = ShapeAtPoint(
            model = TODO(""),
            position = Vector3f(100f, 0f, 0f),
            rotation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            scale = Vector3(1f)
    )

    val lightsLayer = Layer(
            name = "Lights",
            description = "This layer show the illumination of an area",
            rules = listOf(Rule(
                    filter = "ignore",
                    minDistance = 0f,
                    maxDistance = 2000f,
                    shapes = listOf(cube)
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