package tel.schich.tinyjib

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tel.schich.tinyjib.jib.ImageMetadataOutput
import tel.schich.tinyjib.params.OUTPUT_FILE_NAME
import tel.schich.tinyjib.util.escapeKotlinString
import tel.schich.tinyjib.util.executeGradleDefaults
import tel.schich.tinyjib.util.generateProject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class BasicFunctionalityTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun canBuildMinimalTar() {
        val mainClass = "tinyjib.Main"
        generateProject(tempDir, mainClass, "8.2", config = """
            from {
                image = "scratch"
            }
            to {
                image = "test:latest"
            }
            container {
                mainClass = "${escapeKotlinString(mainClass)}"
            }       
        """)
        val result = executeGradleDefaults(tempDir, listOf("tinyJibTar"), javaVersion = "8", 90.seconds)

        println("Exit code: ${result.exitCode}")
        println("Standard output: ${result.stdout}")
        if (result.stderr.isNotEmpty()) {
            println("Standard error: ${result.stderr}")
        }

        assertEquals(0, result.exitCode)

        val buildDir = tempDir.resolve("build")

        val json = Files.newInputStream(tempDir.resolve("$OUTPUT_FILE_NAME.json")).use {
            @OptIn(ExperimentalSerializationApi::class)
            Json.decodeFromStream<ImageMetadataOutput>(it)
        }

        assertEquals(listOf("latest"), json.tags)

        val imageId = Files.readAllBytes(buildDir.resolve("$OUTPUT_FILE_NAME.json")).decodeToString().trim()
        assertEquals(imageId, json.imageId)

        val imageDigest = Files.readAllBytes(buildDir.resolve("$OUTPUT_FILE_NAME.json")).decodeToString().trim()
        assertEquals(imageDigest, json.imageDigest)
    }
}
