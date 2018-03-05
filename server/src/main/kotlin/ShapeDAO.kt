import org.postgis.MultiLineString
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point

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
                        it.z = cota
                        addPoint(it, line)
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

    fun getBuildings(): Model {

        val vertices = mutableListOf<Float>()
        val shapes = mutableListOf<Shape>()

        DDBBManager.useConnection {
            query("SELECT geom FROM \"delimitacion construcciones\" WHERE municipio = '078' LIMIT 1000;").forEach {
                val geom = it.getObject("geom") as PGgeometry

                (geom.geometry as MultiPolygon).polygons.map { polygon ->

                    for (i in 0 until polygon.numRings()) {
                        val ring = polygon.getRing(i)

                        ring.points.forEach {
                            addPoint(it, vertices)
                        }

                        for (j in 1 until ring.numPoints() - 1) {
                            shapes.add(Shape(listOf(0, j, j + 1)))
                        }
                    }
                }
            }
        }

        return Model(vertices, shapes, ShapeType.MESH)
    }

    private fun addPoint(p: Point, vertexList: MutableList<Float>) {
        val minX = 513905.06f
        val minY = 4727060.5f

        val scale = 1 / 100f
        val hScale = 1 / 50f

        vertexList.add(scale * -(p.x.toFloat() - minX))
        vertexList.add(hScale * (p.z.toFloat()))
        vertexList.add(scale * (p.y.toFloat() - minY))
    }
}