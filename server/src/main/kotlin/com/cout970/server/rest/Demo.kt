package com.cout970.server.rest

import com.cout970.server.ddbb.*
import com.cout970.server.rest.Defs.CameraType.PERSPECTIVE
import com.cout970.server.rest.Defs.Rotation
import com.cout970.server.rest.Defs.Scene
import com.cout970.server.rest.Defs.ViewPoint
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.SceneBaker
import com.cout970.server.util.debug
import com.cout970.server.util.getAreaString
import org.joml.Vector3f

lateinit var scene: Scene

fun bakeScene() {
    debug("Baking scene...")
    Rest.cacheMap.clear()
    debug("Baking terrain...")
    TerrainLoader.bakeTerrain()
    scene = SceneBaker.bake(createDemoScene())
    debug("Scene baked")
}

fun createDemoScene(): Scene {

    val area = getAreaString(0 to 0)
    debug("Baking buildings...")
    val buildingLayer = BuildingLayerLoader.load(area)
    debug("Baking streets...")
    val streetsLayer =  StreetLayerLoader.load(area)
    debug("Baking lights...")
    val lightsLayer = LightsLayerLoader.load(area)
    debug("Baking schools...")
    val schoolsLayer = SchoolLayerLoader.load(area)
    debug("Baking lights...")
    val parksLayer = ParkLayerLoader.load(area)
    debug("Baking terrain...")
    val terrainLayer = TerrainLayerLoader.load(area)

    val mainViewPoint = ViewPoint(
            location = Vector3f(0f, 800f, 0f),
            orientation = Rotation(0f, Vector3f(0f, 0f, 0f)),
            camera = PERSPECTIVE
    )

    return Scene(
            title = "Demo scene",
            abstract = "A demo scene showing the base components of a scene",
            viewPoints = listOf(mainViewPoint),
            layers = listOf(streetsLayer, terrainLayer, buildingLayer, lightsLayer, schoolsLayer, parksLayer)
//                    parksLayer, schoolsLayer)
    )
}