import org.postgis.MultiLineString
import org.postgis.PGgeometry

object ShapeDAO {

    fun getMultiLine(mun: String): Model {

        require("""\d\d\d""".toRegex().matches(mun)) { "Invalid municipio code: $mun" }
        val lines = mutableListOf<List<Float>>()

        DDBBManager.useConnection {
            query("SELECT geom, cota FROM testing.curvas WHERE municipio = '$mun' LIMIT 1000;").forEach {
                val cota = it.getDouble("cota")
                val geom = it.getObject("geom") as PGgeometry
                val multiLine = geom.geometry as MultiLineString

                val scale = 1 / 100f
                val hScale = 1 / 50f

                val minX = 513905.06f
                val minY = 4727060.5f

                val line = mutableListOf<Float>()

                multiLine.lines.forEach {
                    it.points.forEach {
                        line.add(scale * -(it.getX().toFloat() - minX))
                        line.add(hScale * (cota.toFloat()))
                        line.add(scale * (it.getY().toFloat() - minY))
                    }
                }

                lines.add(line)
            }
        }

        val vertices = mutableListOf<Float>()
        val shapes = mutableListOf<Shape>()

        lines.forEach {
            val start = vertices.size / 3
            shapes.add(Shape((0 until it.size / 3).map { it + start }))
            vertices.addAll(it)
        }

        return Model(vertices, shapes, ShapeType.LINE)
    }
}