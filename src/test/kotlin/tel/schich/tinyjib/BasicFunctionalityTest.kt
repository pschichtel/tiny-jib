package tel.schich.tinyjib

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import tel.schich.tinyjib.jib.ImageMetadataOutput
import tel.schich.tinyjib.params.OUTPUT_FILE_NAME
import tel.schich.tinyjib.util.MINIMUM_SUPPORTED_GRADLE_VERSION
import tel.schich.tinyjib.util.deleteDockerImage
import tel.schich.tinyjib.util.escapeKotlinString
import tel.schich.tinyjib.util.executeGradleDefaults
import tel.schich.tinyjib.util.fetchConfig
import tel.schich.tinyjib.util.fetchManifest
import tel.schich.tinyjib.util.generateProject
import tel.schich.tinyjib.util.inspectDockerImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.collections.emptyList
import kotlin.time.Duration.Companion.seconds

class BasicFunctionalityTest {
    @TempDir
    lateinit var tempDir: Path

    private fun testCanBuildMinimalImage(task: String, imageName: String, multiplatform: Boolean): Pair<String, String> {
        val mainClass = "tinyjib.Main"

        val platforms = if (multiplatform) {
            """
                platforms {
                  platform {
                    architecture = "amd64"
                    os = "linux"
                  }
                  platform {
                    architecture = "arm64"
                    os = "linux"
                  }
                } 
            """.trimIndent()
        } else {
            ""
        }

        val imageFormat = if (multiplatform) {
            """format = tel.schich.tinyjib.params.TargetImageParameters.ImageFormat.Docker"""
        } else {
            ""
        }

        generateProject(tempDir, mainClass, MINIMUM_SUPPORTED_GRADLE_VERSION, config = """
            from {
                image = "scratch"
                $platforms
            }
            to {
                image = "${escapeKotlinString(imageName)}"
                $imageFormat
            }
            container {
                mainClass = "${escapeKotlinString(mainClass)}"
            }
            allowInsecureRegistries.set(true)
        """)
        val result = executeGradleDefaults(tempDir, listOf(task), javaVersion = "8", 90.seconds)

        println("Exit code: ${result.exitCode}")
        println("Standard output: ${result.stdout}")
        if (result.stderr.isNotEmpty()) {
            println("Standard error: ${result.stderr}")
        }

        assertEquals(0, result.exitCode)

        val buildDir = tempDir.resolve("build")

        val json = Files.newInputStream(buildDir.resolve("$OUTPUT_FILE_NAME.json")).use {
            @OptIn(ExperimentalSerializationApi::class)
            Json.decodeFromStream<ImageMetadataOutput>(it)
        }

        assertEquals(listOf("latest"), json.tags)

        val imageId = Files.readAllBytes(buildDir.resolve("$OUTPUT_FILE_NAME.id")).decodeToString().trim()
        assertEquals(imageId, json.imageId)

        val imageDigest = Files.readAllBytes(buildDir.resolve("$OUTPUT_FILE_NAME.digest")).decodeToString().trim()
        assertEquals(imageDigest, json.imageDigest)

        return imageId to imageDigest
    }

    @Test
    fun canBuildMinimalTar() {
        testCanBuildMinimalImage("tinyJibTar", "test:latest", multiplatform = false)
    }

    @Test
    fun canBuildMinimalDockerImage() {
        val name = "${UUID.randomUUID()}:latest"
        val (imageId, _) = testCanBuildMinimalImage("tinyJibDocker", name, multiplatform = false)
        try {
            val dockerImage = inspectDockerImage(imageId)
            assertEquals(imageId, dockerImage.id)
            assertEquals(emptyList<String>(), dockerImage.repoDigests)
            assertEquals(listOf(name), dockerImage.repoTags)
        } finally {
            deleteDockerImage(imageId)
        }
    }

    @Test
    fun canPublishMinimalImage() {
        val container = GenericContainer("docker.io/library/registry:3.0.0")
            .withExposedPorts(5000)
        container.start()
        val registryPort = container.getMappedPort(5000)
        try {
            val registry = "127.0.0.1:$registryPort"
            val repo = "test/${UUID.randomUUID()}"
            val (_, imageDigest) = testCanBuildMinimalImage("tinyJibPublish", "$registry/$repo:latest", multiplatform = true)
            val manifest = fetchManifest(registry, repo, imageDigest)
            val config = fetchConfig(registry, repo, manifest)
            assertEquals("java", config.config.entrypoint.first())
        } finally {
            container.stop()
        }
    }
}
