import com.mid.varagh.buildlogic.JAVA_VERSION
import com.mid.varagh.buildlogic.configureKotlin
import com.mid.varagh.buildlogic.lib
import com.mid.varagh.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin/JVM module (no Android dependencies). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        configureKotlin()
        dependencies {
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("mockk"))
            add("testImplementation", libs.lib("turbine"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        }
    }
}
