package com.cout970.server.ddbb

import com.cout970.server.Config
import org.postgis.DriverWrapper
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

        /*
        * Add the geometry types to the connection. Note that you
        * must cast the connection to the pgsql-specific connection
        * implementation before calling the addDataType() method.
        */
//        connection.addDataType("geometry", Class.forName("org.postgis.PGgeometry") as Class<PGobject>)
//        connection.addDataType("box3d", Class.forName("org.postgis.PGboxbase") as Class<PGobject>)

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
//        println("Query started: '$sql'")
        val start = System.currentTimeMillis()
        val statement = createStatement()
        val resultSet = statement.executeQuery(sql)
        while (resultSet.next()) {
            yield(resultSet)
        }
        statement.close()
//        println("Query finished in (${System.currentTimeMillis() - start}ms): '$sql'")
    }
}

