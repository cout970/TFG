import org.gradle.kotlin.dsl.kotlin

plugins {
    base
    kotlin("jvm") version "1.2.0" apply false
    id("java")
    id("com.moowork.node") version "1.2.0"
}

allprojects {
    group = "com.cout970"
    version = "0.0.0"

    repositories {
        mavenCentral()
        jcenter()
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("http://download.osgeo.org/webdav/geotools/") }
        maven { url = uri("http://repo.boundlessgeo.com/main") }
    }
}


dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}