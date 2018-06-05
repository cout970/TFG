package com.cout970.server.scene

import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.glTF.Vector2
import com.cout970.server.glTF.Vector3
import com.cout970.server.rest.Rest
import com.cout970.server.util.colorFromHue
import com.cout970.server.util.debug
import com.cout970.server.util.toGeometry
import eu.printingin3d.javascad.basic.Angle
import eu.printingin3d.javascad.coords.Coords3d
import eu.printingin3d.javascad.coords.Dims3d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.LinearExtrude
import eu.printingin3d.javascad.models2d.Polygon
import org.joml.Vector2f
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
            Vector2f(origin.x - 1000, origin.y - 1000),
            Vector2f(2000f)
    )

    val lightModel = Cube.fromCoordinates(
            Coords3d(0.4, 0.0, 0.4),
            Coords3d(0.6, 4.0, 0.6)
    ).addModel(Cube.fromCoordinates(
            Coords3d(0.0, 4.0, 0.0),
            Coords3d(1.0, 5.0, 1.0)
    )).toGeometry()

    val parkModel = Cube(Dims3d(1.0, 10.0, 1.0)).toGeometry()

    val schoolLabels = DLabelShapeSource(
            labelSource = DLabelSource(
                    geomField = "geom",
                    textField = "nombre",
                    tableName = "centros de enseñanza (polígono)",
                    area = area
            ),
            scale = 8f,
            material = DMaterial(
                    metallic = 0.5f,
                    roughness = 0.5f,
                    diffuseColor = DColor(1f, 1f, 1f),
                    emissiveColor = DColor(1f, 1f, 1f),
                    opacity = 1f
            ),
            projection = DefaultGroundProjection(50f, false)
    )

    val schools = DExtrudeShapeSource(
            polygonsSource = DPolygonsSource(
                    geomField = "geom",
                    tableName = "centros de enseñanza (polígono)",
                    area = area
            ),
            height = 25f,
            material = DMaterial(
                    metallic = 0.0f,
                    roughness = 0.0f,
                    diffuseColor = colorFromHue(180f / 360f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    opacity = 0.35f
            ),
            projection = DefaultGroundProjection(0f, false)
    )

    val parks = DShapeAtSurfaceSource(
            geometrySource = DInlineSource(parkModel),
            surfaceSource = DPolygonsSource(
                    "geom", "parques (polígono)", area
            ),
            resolution = 0.01f,
            material = DMaterial(
                    metallic = 0.0f,
                    roughness = 0.5f,
                    diffuseColor = DColor(0f, 0.5f, 0f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    opacity = 1f
            ),
            projection = DefaultGroundProjection(5f, false)
    )

    val lights = DShapeAtPointSource(
            points = DPointSource("geom", "puntos de luz", area),
            geometrySource = DInlineSource(lightModel),
            material = DMaterial(
                    metallic = 0.1f,
                    roughness = 0.3f,
                    diffuseColor = DColor(1f, 1f, 0.0f),
                    emissiveColor = DColor(0.1f, 0.1f, 0.1f),
                    opacity = 1f
            ),
            projection = DefaultGroundProjection(0f, false)
    )

    val streets = DExtrudeShapeSource(
            polygonsSource = DPolygonsSource(
                    geomField = "geom",
                    tableName = "calles",
                    area = area
            ),
            height = 5f,
            material = DMaterial(
                    metallic = 0.0f,
                    roughness = 0.8f,
                    diffuseColor = colorFromHue(63.1f / 360f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    opacity = 1f
            ),
            projection = SnapProjection(-4.0f)
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
                    metallic = 0.0f,
                    roughness = 0.75f,
                    diffuseColor = colorFromHue(273.1f / 360f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    opacity = 1f
            ),
            projection = DefaultGroundProjection(1f, false)
    )

    val schoolsLabelsLayer = DLayer(
            name = "School names",
            description = "Description",
            rules = listOf(DRule(
                    properties = listOf(
                            DPropertyFollowCamera
                    ),
                    shapes = listOf(schoolLabels)
            ))
    )

    val schoolsLayer = DLayer(
            name = "Schools",
            description = "Description",
            rules = listOf(DRule(
                    properties = emptyList(),
                    shapes = listOf(schools)
            ))
    )

    val parksLayer = DLayer(
            name = "Parks",
            description = "Description",
            rules = listOf(DRule(
                    properties = emptyList(),
                    shapes = listOf(parks)
            ))
    )

    val lightsLayer = DLayer(
            name = "Lights",
            description = "Description",
            rules = listOf(DRule(
                    properties = emptyList(),
                    shapes = listOf(lights)
            ))
    )

    val streetLayer = DLayer(
            name = "Streets",
            description = "Description",
            rules = listOf(DRule(
                    properties = emptyList(),
                    shapes = listOf(streets)
            ))
    )

    val buildingLayer = DLayer(
            name = "Buildings",
            description = "Description",
            rules = listOf(DRule(
                    properties = emptyList(),
                    shapes = listOf(buildings)
            ))
    )

    val mainViewPoint = DViewPoint(
            location = Vector3(-6.6f, 213.1f, -31.6f),
            orientation = Vector3(0.066f, 0.427f, 0.901f),
            camera = DCameraType.PERSPECTIVE
    )

    val ground = DGround(
            file = "../data/height/PNOA_MDT05_ETRS89_HU29_0094_LID.tif",
            material = DMaterial(
                    metallic = 0.0f,
                    roughness = 0.5f,
                    diffuseColor = DColor(0.0f, 1.0f, 0.0f),
                    emissiveColor = DColor(0f, 0f, 0f),
                    opacity = 1.0f
            ),
            area = area,
            gridSize = 5f
    )

    val scene = DScene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(buildingLayer, streetLayer, lightsLayer, parksLayer, schoolsLayer, schoolsLabelsLayer, debugLayer()),
            ground = ground,
            origin = origin
    )

    File("test.json").writeText(GLTF_GSON.toJson(scene))
    return scene
}

fun debugLayer(): DLayer {

    val cubeA = LinearExtrude(Polygon(listOf(
            Coords2d(0.0, 0.0), Coords2d(10.0, 0.0),
            Coords2d(10.0, 10.0), Coords2d(0.0, 10.0)
    )), 1.0, Angle.ZERO, 1.0)

    val cubeB = LinearExtrude(Polygon(listOf(
            Coords2d(1.0, 1.0), Coords2d(9.0, 1.0),
            Coords2d(9.0, 9.0), Coords2d(1.0, 9.0)
    )), 2.0, Angle.ZERO, 1.0)

    return DLayer(
            name = "School names",
            description = "Description",
            rules = listOf(DRule(
                    properties = listOf(DPropertyFollowCamera),
                    shapes = listOf(
                            DInlineShapeSource(ShapeLabel(
                                    txt = "Cube",
                                    scale = 4f,
                                    material = DMaterial(
                                            metallic = 0.5f,
                                            roughness = 0.5f,
                                            diffuseColor = DColor(1f, 1f, 1f),
                                            emissiveColor = DColor(1f, 1f, 1f),
                                            opacity = 1f
                                    ),
                                    projection = DefaultGroundProjection(100f, true),
                                    position = Vector2(0f, 0f)
                            )),
                            DInlineShapeSource(ShapeLabel(
                                    txt = "Union",
                                    scale = 4f,
                                    material = DMaterial(
                                            metallic = 0.5f,
                                            roughness = 0.5f,
                                            diffuseColor = DColor(1f, 1f, 1f),
                                            emissiveColor = DColor(1f, 1f, 1f),
                                            opacity = 1f
                                    ),
                                    projection = DefaultGroundProjection(100f, true),
                                    position = Vector2(0f, 50f)
                            )),
                            DInlineShapeSource(ShapeLabel(
                                    txt = "Difference",
                                    scale = 4f,
                                    material = DMaterial(
                                            metallic = 0.5f,
                                            roughness = 0.5f,
                                            diffuseColor = DColor(1f, 1f, 1f),
                                            emissiveColor = DColor(1f, 1f, 1f),
                                            opacity = 1f
                                    ),
                                    projection = DefaultGroundProjection(100f, true),
                                    position = Vector2(0f, 100f)
                            )),
                            DInlineShapeSource(ShapeLabel(
                                    txt = "Intersection",
                                    scale = 4f,
                                    material = DMaterial(
                                            metallic = 0.5f,
                                            roughness = 0.5f,
                                            diffuseColor = DColor(1f, 1f, 1f),
                                            emissiveColor = DColor(1f, 1f, 1f),
                                            opacity = 1f
                                    ),
                                    projection = DefaultGroundProjection(100f, true),
                                    position = Vector2(0f, 150f)
                            ))
                    )
            ), DRule(
                    properties = emptyList(),
                    shapes = listOf(
                            DInlineShapeSource(
                                    ShapeAtPoint(
                                            point = Vector2(0f, 0f),
                                            projection = DefaultGroundProjection(90f, true),
                                            material = DMaterial(
                                                    metallic = 0.5f,
                                                    roughness = 0.5f,
                                                    diffuseColor = DColor(1f, 0.5f, 0.5f),
                                                    emissiveColor = DColor(0f, 0f, 0f),
                                                    opacity = 1f
                                            ),
                                            geometry = cubeA.toGeometry()
                                    )
                            ),
                            DInlineShapeSource(
                                    ShapeAtPoint(
                                            point = Vector2(0f, 50f),
                                            projection = DefaultGroundProjection(90f, true),
                                            material = DMaterial(
                                                    metallic = 0.5f,
                                                    roughness = 0.5f,
                                                    diffuseColor = DColor(1f, 0f, 0f),
                                                    emissiveColor = DColor(0f, 0f, 0f),
                                                    opacity = 1f
                                            ),
                                            geometry = cubeA.toCSG().union(cubeB.toCSG()).polygons.toGeometry()
                                    )
                            ),
                            DInlineShapeSource(
                                    ShapeAtPoint(
                                            point = Vector2(0f, 100f),
                                            projection = DefaultGroundProjection(90f, true),
                                            material = DMaterial(
                                                    metallic = 0.5f,
                                                    roughness = 0.5f,
                                                    diffuseColor = DColor(0f, 1f, 0f),
                                                    emissiveColor = DColor(0f, 0f, 0f),
                                                    opacity = 1f
                                            ),
                                            geometry = cubeA.toCSG().difference(cubeB.toCSG()).polygons.toGeometry()
                                    )
                            ),
                            DInlineShapeSource(
                                    ShapeAtPoint(
                                            point = Vector2(0f, 150f),
                                            projection = DefaultGroundProjection(90f, true),
                                            material = DMaterial(
                                                    metallic = 0.5f,
                                                    roughness = 0.5f,
                                                    diffuseColor = DColor(1f, 0f, 0f),
                                                    emissiveColor = DColor(0f, 0f, 0f),
                                                    opacity = 1f
                                            ),
                                            geometry = cubeA.toCSG().intersect(cubeB.toCSG()).polygons.toGeometry()
                                    )
                            ))
            ))
    )
}