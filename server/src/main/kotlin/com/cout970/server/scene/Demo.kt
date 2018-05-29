package com.cout970.server.scene

import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.glTF.Vector2
import com.cout970.server.rest.Rest
import com.cout970.server.util.colorFromHue
import com.cout970.server.util.debug
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.models.Cube
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
            Vector2f(origin.x - 4000, origin.y - 4000),
            Vector2f(8000f)
    )

    val lightModel = Cube.fromCoordinates(
            Coords3d(0.4, 0.0, 0.4),
            Coords3d(0.6, 4.0, 0.6)
    ).addModel(Cube.fromCoordinates(
            Coords3d(0.0, 4.0, 0.0),
            Coords3d(1.0, 5.0, 1.0)
    )).toGeometry()

    val lights = DShapeAtPointSource(
            points = DPointSource("geom", "puntos de luz", area),
            geometrySource = DInlineSource(lightModel),
            material = DMaterial(
                    ambientIntensity = 0.5f,
                    shininess = 0f,
                    diffuseColor = DColor(1f, 1f, 0.0f),
                    emissiveColor = DColor(0.1f, 0.1f, 0.1f),
                    specularColor = DColor(.1f, 1f, 1f),
                    transparency = 0f
            ),
            projection = DefaultGroundProjection(0f, false)
    )

    val streets = DPolygonsShapeSource(
            geometrySource = DPolygonsSource(
                    geomField = "geom",
                    tableName = "calles",
                    area = area
            ),
            material = DMaterial(
                    ambientIntensity = 0.5f,
                    shininess = 0f,
                    diffuseColor = colorFromHue(63.1f / 360f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    specularColor = DColor(1f, 1f, 1f),
                    transparency = 0f
            ),
            projection = SnapProjection(0.5f)
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

    val lightsLayer = DLayer(
            name = "Lights",
            description = "Description",
            rules = listOf(DRule(
                    filter = "none",
                    minDistance = 0f,
                    maxDistance = 10f,
                    shapes = listOf(lights)
            ))
    )

    val streetLayer = DLayer(
            name = "Streets",
            description = "Description",
            rules = listOf(DRule(
                    filter = "none",
                    minDistance = 0f,
                    maxDistance = 10f,
                    shapes = listOf(streets)
            ))
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
            layers = listOf(buildingLayer, streetLayer, lightsLayer),
            ground = ground,
            origin = origin
    )

    File("test.json").writeText(GLTF_GSON.toJson(s))
    return s
}