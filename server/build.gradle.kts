import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.js.translate.context.Namer.kotlin
import java.util.regex.Pattern.compile

val kotlinVersion = "1.2.21"
val kotlinCoroutinesVersion = "0.22.2"
val jomlVersion = "1.9.8"
val sparkVersion = "1.0.0-alpha"
val postgisVersion = "2.1.7.2"
val postgresqlVersion = "42.2.1"
val junitVersion = "4.12"
val gsonVersion = "2.8.2"
val geotoolsVersion = "2.7.7-NB72"
val jcsgVersion = "0.5.6"
val gdalVersion = "1.11.2"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    compile("org.joml:joml:$jomlVersion")
    compile("com.sparkjava:spark-kotlin:$sparkVersion")
    compile("net.postgis:postgis-jdbc:$postgisVersion")
    compile("org.postgresql:postgresql:$postgresqlVersion")
    compile("com.google.code.gson:gson:$gsonVersion")
    compile("nl.cloudfarming.client:lib-geotools:$geotoolsVersion")
    compile("org.gdal:gdal:$gdalVersion")
    compile("eu.printingin3d.javascad:javascad:0.9.0.12")


    testCompile("junit:junit:$junitVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testCompile("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

configure<KotlinProjectExtension> {
    experimental.coroutines = Coroutines.ENABLE
}