
import org.postgis.DriverWrapper
import org.postgresql.Driver
import org.postgresql.PGConnection
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.coroutines.experimental.buildSequence


object DDBBManager {

    fun init() {
        // Load the driver

        Driver.isRegistered()
        DriverWrapper.isRegistered()
        DriverManager.getConnection(Config.JDBC_URL, "postgres", System.getenv("postgres_passwd")).close()
    }

    inline fun useConnection(func: Connection.() -> Unit) {
        val connection = DriverManager.getConnection(Config.JDBC_URL, "postgres", System.getenv("postgres_passwd"))

        if (connection !is PGConnection)
            throw IllegalStateException("Invalid connection type: ${connection::class.java}, for $connection")

        /*
        * Add the geometry types to the connection. Note that you
        * must cast the connection to the pgsql-specific connection
        * implementation before calling the addDataType() method.
        */
        connection.addDataType("geometry", Class.forName("org.postgis.PGgeometry") as Class<PGobject>)
        connection.addDataType("box3d", Class.forName("org.postgis.PGboxbase") as Class<PGobject>)

        connection.func()
        connection.close()
    }

}

//language=PostgreSQL
fun Connection.query(sql: String): Sequence<ResultSet> = buildSequence {
    val statement = createStatement()
    val resultSet = statement.executeQuery(sql)
    while (resultSet.next()) {
        yield(resultSet)
    }
    statement.close()
}