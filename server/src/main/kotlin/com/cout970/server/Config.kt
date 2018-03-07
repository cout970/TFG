package com.cout970.server

object Config {
    val DDBB_HOST = System.getProperty("DDBB_HOST") ?: "localhost"
    val DDBB_POST = 5432
    val DDBB_NAME = "postgres"

    val JDBC_URL = "jdbc:postgresql://$DDBB_HOST:$DDBB_POST/$DDBB_NAME"
}