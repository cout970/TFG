object Defs {
    data class Scene(
            val camera: Camera,
            val layers: List<Layer>,
    val materials: List<Material>
    )

    sealed class Camera {
        data class PerspectiveCamera(val fov: Float, val aspect: Float, val near: Float, val far: Float) : Camera()
        data class OrthograficCamera(val sizeX: Float, val sizeZ: Float, val near: Float, val far: Float) : Camera()
    }

    data class Layer(val name: String)

    data class Material(
            val baseColor: List<Float>,// Vec4
            val normalTexture: String,
            val emissiveFactor: Float
    )
}