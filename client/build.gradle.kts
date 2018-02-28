import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.js.translate.context.Namer.kotlin

apply {
    plugin("kotlin")
    plugin("com.moowork.node")
}

the<JavaPluginConvention>().sourceSets {
    "main" {
        java {
            srcDirs("src")
        }
    }
}

dependencies {}
