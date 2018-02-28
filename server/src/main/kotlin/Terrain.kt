import org.joml.SimplexNoise

/**
 *
 */
private fun generateHeightMapAux(x: Int, y: Int): HeightMap {
    val size = 33
    val map = HeightMap(MutableList(size * size) { 0f }, size, size)

    for (i in 0 until size) {
        for (j in 0 until size) {
            val noise = SimplexNoise.noise(i / 32f + x, j / 32f + y)
            val base = noise * 0.5f + 1f
            map[i, j] = base * 10
        }
    }
    return map
}

fun generateDebugHeightMap(x: Int, y: Int): Model {
    val map = generateHeightMapAux(x, y)
    val vertices = mutableListOf<Float>()
    val shapes = mutableListOf<Shape>()

    val offsetX = x * (map.width - 1)
    val offsetY = y * (map.height - 1)

    for (i in 0 until map.width) {
        for (j in 0 until map.height) {
            vertices.add(i.toFloat() + offsetX)
            vertices.add(map[i, j])
            vertices.add(j.toFloat() + offsetY)
        }
    }

    for (i in 0 until map.width - 1) {
        for (j in 0 until map.height - 1) {
            val a = i + j * map.width
            val b = a + 1
            val c = a + 1 * map.width
            val d = a + 1 + 1 * map.width

            shapes.add(Shape(listOf(a, b, c)))
            shapes.add(Shape(listOf(b, d, c)))
        }
    }

    return Model(vertices, shapes, ShapeType.MESH)
}

