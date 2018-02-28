import org.joml.Vector3f

data class Multiline(val lines: List<List<Vector3f>>)

enum class ShapeType { LINE, MESH }

data class Shape(val indices: List<Int>)

data class Model(val vertex: List<Float>, val shapes: List<Shape>, val type: ShapeType)