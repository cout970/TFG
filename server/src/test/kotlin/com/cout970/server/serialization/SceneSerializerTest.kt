package com.cout970.server.serialization

import com.cout970.server.scene.DScene
import com.cout970.server.scene.createDemoScene
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneSerializerTest {

    @Test
    fun `Serialization should be symmetric`(){
        val scene = createDemoScene()

        val json = SCENE_GSON.toJson(scene)
        val scene2 = SCENE_GSON.fromJson(json, DScene::class.java)

        assertEquals(scene.toString(), scene2.toString())
    }
}