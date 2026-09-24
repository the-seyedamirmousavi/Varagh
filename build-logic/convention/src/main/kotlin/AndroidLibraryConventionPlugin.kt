import com.android.build.api.dsl.LibraryExtension
import com.mid.varagh.buildlogic.configureAndroidLibrary
import com.mid.varagh.buildlogic.lib
import com.mid.varagh.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Android library with AGP built-in Kotlin support and the standard unit-test stack. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> { configureAndroidLibrary(this) }
        dependencies {
            add("testImplementation", libs.lib("junit"))
            add("testImplementation", libs.lib("mockk"))
            add("testImplementation", libs.lib("turbine"))
            add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("androidx-test-core"))
            add("testImplementation", libs.lib("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.lib("androidx-test-runner"))
            add("androidTestImplementation", libs.lib("androidx-test-core"))
            add("androidTestImplementation", libs.lib("androidx-test-ext-junit"))
        }
    }
}
