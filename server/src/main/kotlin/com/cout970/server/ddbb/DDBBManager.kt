package com.cout970.server.ddbb

import com.cout970.server.Config
import com.cout970.server.glTF.Vector2
import com.cout970.server.scene.DArea
import com.cout970.server.scene.DPolygon
import com.cout970.server.terrain.TerrainLoader
import com.cout970.server.util.info
import com.cout970.server.util.relativize
import com.cout970.server.util.toPolygon
import com.cout970.server.util.toSQL
import org.postgis.*
import org.postgresql.Driver
import org.postgresql.PGConnection
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence


object DDBBManager {

    fun init() {
        // Load the driver

        Driver.isRegistered()
        DriverWrapper.isRegistered()
        DriverManager.getConnection(Config.JDBC_URL, Config.DDBB_USER, Config.DDBB_PASSWORD()).close()
    }

    inline fun <T> useConnection(func: Connection.() -> T): T {
        val connection = DriverManager.getConnection(Config.JDBC_URL, Config.DDBB_USER, Config.DDBB_PASSWORD())

        if (connection !is PGConnection)
            throw IllegalStateException("Invalid connection type: ${connection::class.java}, for $connection")

        val result = connection.func()
        connection.close()
        return result
    }

    fun <T> load(sql: String, mapFunc: (ResultSet) -> T): List<T> {
        return DDBBManager.useConnection {
            query(sql).map(mapFunc).toList()
        }
    }

    fun Connection.query(sql: String): Sequence<ResultSet> = buildSequence {
        info("Query started: '${sql.trimIndent().replace("\n", " ")}'")
        val start = System.currentTimeMillis()
        val statement = createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            yield(resultSet)
        }
        statement.close()
        info("Query finished in (${System.currentTimeMillis() - start}ms)")
    }

    fun loadPolygons(geomField: String, tableName: String, area: DArea): List<PolygonGroup> {
        val sql = """
                SELECT ST_Segmentize(inter, 5)
                FROM "$tableName", ${area.toSQL()} AS area, ST_Intersection($geomField, area) AS inter
                WHERE ST_Isvalid($geomField) AND ST_Intersects($geomField, area);
                """

        return DDBBManager.load(sql) {

            val geom = it.getObject(1) as PGgeometry

            val geometry = geom.geometry

            when (geometry) {
                is MultiPolygon -> PolygonGroup(geometry.polygons.map { poly -> poly.toPolygon().relativize() })
                is Polygon -> PolygonGroup(listOf(geometry.toPolygon().relativize()))
                else -> error("Unknown Geometry type: ${geometry::class.java}, $geometry")
            }
        }
    }

    fun loadExtrudedPolygons(geomField: String, heightField: String, tableName: String, heightScale: Float, area: DArea): List<ExtrudePolygonGroup> {
        val sql = """
                SELECT $geomField, $heightField
                FROM "$tableName", ${area.toSQL()} AS area
                WHERE ST_Intersects($geomField, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject(geomField) as PGgeometry
            val height = it.getFloat(heightField)
            val multiPolygon = geom.geometry as MultiPolygon

            ExtrudePolygonGroup(
                    polygons = multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() },
                    height = height * heightScale
            )
        }
    }

    fun loadLabels(geomField: String, nameField: String, tableName: String, area: DArea): List<Label> {
        val sql = """
                SELECT $nameField, center
                FROM "$tableName", ${area.toSQL()} AS area, ST_Centroid($geomField) as center
                WHERE ST_Intersects($geomField, area);
                      """

        return DDBBManager.load(sql) {

            val name = it.getString(nameField)
            val centerGeom = it.getObject("center") as PGgeometry
            val center = centerGeom.geometry as Point

            Label(
                    text = name,
                    pos = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
            )
        }
    }

    fun loadPoints(geomField: String, tableName: String, area: DArea): List<Vector2> {
        val sql = """
                SELECT geom
                FROM "$tableName", ${area.toSQL()} AS area
                WHERE ST_Intersects($geomField, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject("geom") as PGgeometry
            val point = geom.geometry as Point

            Vector2(
                    point.x.toFloat() - TerrainLoader.ORIGIN.x,
                    point.y.toFloat() - TerrainLoader.ORIGIN.z
            )
        }
    }

    data class PolygonGroup(val polygons: List<DPolygon>)
    data class ExtrudePolygonGroup(val polygons: List<DPolygon>, val height: Float)
    data class Label(val text: String, val pos: Vector2)
}

