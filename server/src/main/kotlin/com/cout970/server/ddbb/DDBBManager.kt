package com.cout970.server.ddbb

import com.cout970.server.Config
import com.cout970.server.rest.Defs
import com.cout970.server.rest.Vector2
import com.cout970.server.util.info
import com.cout970.server.util.relativize
import com.cout970.server.util.toPolygon
import org.postgis.DriverWrapper
import org.postgis.MultiPolygon
import org.postgis.PGgeometry
import org.postgis.Point
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
        info("Query started")
        val start = System.currentTimeMillis()
        val statement = createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            yield(resultSet)
        }
        statement.close()
        info("Query finished in (${System.currentTimeMillis() - start}ms)")
    }

    fun loadPolygons(geomField: String, tableName: String, area: Area): List<PolygonGroup> {
        val sql = """
                SELECT $geomField
                FROM "$tableName", $area AS area
                WHERE ST_Within($geomField, area);
                      """

        return DDBBManager.load(sql) {

            val geom = it.getObject(geomField) as PGgeometry
            val multiPolygon = geom.geometry as MultiPolygon

            PolygonGroup(multiPolygon.polygons.map { poly -> poly.toPolygon().relativize() })
        }
    }

    fun loadExtrudedPolygons(geomField: String, heightField: String, tableName: String, heightScale: Float, area: Area): List<ExtrudePolygonGroup> {
        val sql = """
                SELECT $geomField, $heightField
                FROM "$tableName", $area AS area
                WHERE ST_Within($geomField, area);
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

    fun loadLabels(geomField: String, tableName: String, area: Area): List<Label> {
        val sql = """
                SELECT name, center
                FROM "$tableName", $area AS area, ST_Centroid($geomField) as center
                WHERE ST_Within($geomField, area);
                      """

        return DDBBManager.load(sql) {

            val name = it.getString("name")
            val centerGeom = it.getObject("center") as PGgeometry
            val center = centerGeom.geometry as Point

            Label(
                    text = name,
                    pos = Vector2(center.x.toFloat(), center.y.toFloat()).relativize()
            )
        }
    }

    data class PolygonGroup(val polygons: List<Defs.Polygon>)
    data class ExtrudePolygonGroup(val polygons: List<Defs.Polygon>, val height: Float)
    data class Label(val text: String, val pos: Vector2)
}

