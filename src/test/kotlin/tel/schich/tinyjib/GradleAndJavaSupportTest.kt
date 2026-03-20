package tel.schich.tinyjib

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tel.schich.tinyjib.util.executeGradleDefaults
import tel.schich.tinyjib.util.generateProject
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import tel.schich.tinyjib.util.getProp

class GradleAndJavaSupportTest {
    @TempDir
    lateinit var tempDir: Path

    private fun appliesOnGradleVersion(gradleVersion: String, javaVersion: String) {
        val mainClass = "tinyjib.Main"
        generateProject(tempDir, mainClass, gradleVersion, config = "")
        val result = executeGradleDefaults(tempDir, listOf("tasks"), javaVersion, 90.seconds)

        println("Exit code: ${result.exitCode}")
        println("Standard output: ${result.stdout}")
        if (result.stderr.isNotEmpty()) {
            println("Standard error: ${result.stderr}")
        }

        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("tinyJibTar"))
        assertTrue(result.stdout.contains("tinyJibDocker"))
        assertTrue(result.stdout.contains("tinyJibPublish"))
        assertEquals("", result.stderr)
    }

    @Test
    fun pluginAppliesOnGradle8WithJava8() {
        appliesOnGradleVersion(gradleVersion = "8.0", javaVersion = "8")
    }

    @Test
    fun pluginAppliesOnGradle8WithJava11() {
        appliesOnGradleVersion(gradleVersion = "8.0", javaVersion = "11")
    }

    @Test
    fun pluginAppliesOnGradle8WithJava17() {
        appliesOnGradleVersion(gradleVersion = "8.0", javaVersion = "17")
    }

    @Test
    fun pluginAppliesOnGradle8WithJava21() {
        appliesOnGradleVersion(gradleVersion = "8.0", javaVersion = "21")
    }

    // Gradle 8.x doesn't understand java 25 versions
    // @Test
    // fun pluginAppliesOnGradle8WithJava25() {
    //     appliesOnGradleVersion(gradleVersion = "8.0", javaVersion = "25")
    // }

    @Test
    fun pluginAppliesOnLatestGradle8WithJava8() {
        appliesOnGradleVersion(gradleVersion = "8.14.4", javaVersion = "8")
    }

    @Test
    fun pluginAppliesOnLatestGradle8WithJava11() {
        appliesOnGradleVersion(gradleVersion = "8.14.4", javaVersion = "11")
    }

    @Test
    fun pluginAppliesOnLatestGradle8WithJava17() {
        appliesOnGradleVersion(gradleVersion = "8.14.4", javaVersion = "17")
    }

    @Test
    fun pluginAppliesOnLatestGradle8WithJava21() {
        appliesOnGradleVersion(gradleVersion = "8.14.4", javaVersion = "21")
    }

    // Gradle 8.x doesn't understand java 25 versions
    // @Test
    // fun pluginAppliesOnLatestGradle8WithJava25() {
    //     appliesOnGradleVersion(gradleVersion = "8.14.4", javaVersion = "25")
    // }

    @Test
    fun pluginAppliesOnGradle9WithJava17() {
        appliesOnGradleVersion(gradleVersion = "9.0", javaVersion = "17")
    }

    @Test
    fun pluginAppliesOnGradle9WithJava21() {
        appliesOnGradleVersion(gradleVersion = "9.0", javaVersion = "21")
    }

    @Test
    fun pluginAppliesOnGradle9WithJava25() {
        appliesOnGradleVersion(gradleVersion = "9.0", javaVersion = "25")
    }

    @Test
    fun pluginAppliesOnLatestGradleWithJava17() {

        appliesOnGradleVersion(gradleVersion = getProp("gradleVersion"), javaVersion = "17")
    }

    @Test
    fun pluginAppliesOnLatestGradleWithJava21() {
        appliesOnGradleVersion(gradleVersion = getProp("gradleVersion"), javaVersion = "21")
    }

    @Test
    fun pluginAppliesOnLatestGradleWithJava25() {
        appliesOnGradleVersion(gradleVersion = getProp("gradleVersion"), javaVersion = "25")
    }
}
