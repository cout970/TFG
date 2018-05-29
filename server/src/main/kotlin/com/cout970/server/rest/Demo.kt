package com.cout970.server.rest

import com.cout970.server.ddbb.BuildingLayerLoader
import com.cout970.server.ddbb.TerrainLayerLoader
import com.cout970.server.glTF.GLTF_GSON
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.debug
import com.cout970.server.util.getAreaString
import org.joml.Vector3f
import java.io.File

lateinit var scene: DScene

fun bakeScene() {
    debug("Baking scene...")
    Rest.cacheMap.clear()
    debug("Baking terrain...")
    scene = SceneBaker.bake(createDemoScene())
    debug("Scene baked")
}

fun createDemoScene(): DScene {

    val area = getAreaString(0 to 0)
    debug("Baking buildings...")
    val buildingLayer = BuildingLayerLoader.load(area)
//    debug("Baking streets...")
//    val streetsLayer =  StreetLayerLoader.load(area)
//    debug("Baking lights...")
//    val lightsLayer = LightsLayerLoader.load(area)
//    debug("Baking schools...")
//    val schoolsLayer = SchoolLayerLoader.load(area)
//    debug("Baking lights...")
//    val parksLayer = ParkLayerLoader.load(area)
    debug("Baking terrain...")
    val terrainLayer = TerrainLayerLoader.load(area)

    val mainViewPoint = DViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = DRotation(0f, Vector3f(0f, 0f, 0f)),
            camera = DCameraType.PERSPECTIVE
    )

    val s = DScene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(terrainLayer, buildingLayer)
//                    parksLayer, schoolsLayer)
    )

    File("test.json").writeText(GLTF_GSON.toJson(s))
    return s
}