package com.cout970.server.rest

import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.glTF.Vector2
import com.cout970.server.util.colorFromHue
import com.cout970.server.util.debug
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.File

lateinit var scene: DScene

fun bakeScene() {
    debug("Baking scene...")
    Rest.registerScene(createDemoScene())
    debug("Scene baked")
}

fun createDemoScene(): DScene {

    val origin = Vector2(535909f, 4746842f)

    val area = DArea(
            Vector2f(origin.x + -4000, origin.y + -4000),
            Vector2f(8000f)
    )

    val buildings = DExtrudedShapeSource(
            polygonsSource = DExtrudedPolygonsSource(
                    geomField = "geom",
                    heightField = "plantas",
                    tableName = "edificación alturas",
                    heightScale = 3.5f,
                    area = area
            ),
            material = DMaterial(
                    ambientIntensity = 0.5f,
                    shininess = 0f,
                    diffuseColor = colorFromHue(273.1f / 360f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    specularColor = DColor(1f, 1f, 1f),
                    transparency = 0f
            ),
            projection = DefaultGroundProjection(1f, false)
    )

    val buildingLayer = DLayer(
            name = "Buildings",
            description = "Description",
            rules = listOf(DRule(
                    filter = "none",
                    minDistance = 0f,
                    maxDistance = 10f,
                    shapes = listOf(buildings)
            ))
    )

    val mainViewPoint = DViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = DRotation(0f, Vector3f(0f, 0f, 0f)),
            camera = DCameraType.PERSPECTIVE
    )

    val ground = DGround("../data/GaliciaDTM25m.tif", DMaterial(
            ambientIntensity = 0.0f,
            shininess = 0f,
            diffuseColor = DColor(0.0f, 1.0f, 0.0f),
            emissiveColor = DColor(0f, 0f, 0f),
            specularColor = DColor(0f, 0f, 0f),
            transparency = 0.0f
    ))

    val s = DScene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(buildingLayer),
            ground = ground,
            origin = origin
    )

    File("test.json").writeText(GLTF_GSON.toJson(s))
    return s
}