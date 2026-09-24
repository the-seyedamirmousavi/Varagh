import com.android.build.api.dsl.ApplicationExtension
import com.mid.varagh.buildlogic.configureAndroidApplication
import com.mid.varagh.buildlogic.lib
import com.mid.varagh.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Android application with AGP built-in Kotlin support. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> { configureAndroidApplication(this) }
        dependencies {
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("mockk"))
            add("testImplementation", libs.lib("turbine"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("androidx-test-core"))
            add("testImplementation", libs.lib("androidx-test-ext-junit"))
        }
    }
}
