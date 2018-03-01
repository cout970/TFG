

object Config {
    val DDBB_HOST = "192.168.8.107"
    val DDBB_POST = 5432
    val DDBB_NAME = "postgres"

    val JDBC_URL = "jdbc:postgresql://$DDBB_HOST:$DDBB_POST/$DDBB_NAME"
}