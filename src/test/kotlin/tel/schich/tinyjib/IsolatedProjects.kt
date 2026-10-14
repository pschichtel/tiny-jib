package tel.schich.tinyjib

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tel.schich.tinyjib.util.escapeKotlinString
import tel.schich.tinyjib.util.executeGradleDefaults
import tel.schich.tinyjib.util.generateProject
import tel.schich.tinyjib.util.getProp
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class IsolatedProjects {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun runsWithIsolatedProjects() {
        generateProject(tempDir, "tinyjib.Main", getProp("gradleVersion"), config = """
            from {
                image = "scratch"
            }
            to {
                image = "${escapeKotlinString("test:latest")}"
            }
            container {
                mainClass = "${escapeKotlinString("tinyjib.Main")}"
            }
            allowInsecureRegistries.set(true)
        """)
        val result = executeGradleDefaults(tempDir, listOf("tinyJibTar", "--isolated-projects"), javaVersion = "25", 90.seconds)

        assertEquals(0, result.exitCode)
        assertTrue(result.stdout.contains("BUILD SUCCESSFUL"))
    }
}