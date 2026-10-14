package tel.schich.tinyjib

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import tel.schich.tinyjib.jib.ImageMetadataOutput
import tel.schich.tinyjib.params.OUTPUT_FILE_NAME
import tel.schich.tinyjib.util.ExecuteResult
import tel.schich.tinyjib.util.ImageManifest
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

    private fun runGradle(task: String, imageName: String, multiplatform: Boolean): ExecuteResult {
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
        return executeGradleDefaults(tempDir, listOf(task), javaVersion = "8", 90.seconds)
    }

    private fun testCanBuildMinimalImage(task: String, imageName: String, multiplatform: Boolean): Pair<String, String> {
        val result = runGradle(task, imageName, multiplatform)

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
    fun canBuildMinimalOciTar() {
        testCanBuildMinimalImage("tinyJibTar", "test:latest", multiplatform = false)
    }

    @Test
    fun canBuildMinimalDockerTar() {
        val result = runGradle("tinyJibTar", "test:latest", multiplatform = true)
        assertEquals(1, result.exitCode)
        assertTrue(result.stderr.contains("Caused by: com.google.common.base.VerifyException: multi-platform image building not supported when building a local tar image"))
    }

    private fun testDockerImageBuild(multiplatform: Boolean) {
        val name = "${UUID.randomUUID()}:latest"
        val (imageId, _) = testCanBuildMinimalImage("tinyJibDocker", name, multiplatform)
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
    fun canBuildMinimalOciImageToDocker() {
        testDockerImageBuild(multiplatform = false)
    }

    @Test
    fun canBuildMinimalDockerImageToDocker() {
        testDockerImageBuild(multiplatform = true)
    }

    private fun testImagePublish(multiplatform: Boolean) {
        val container = GenericContainer("docker.io/library/registry:3.0.0")
            .withExposedPorts(5000)
        container.start()
        val registryPort = container.getMappedPort(5000)
        try {
            val registry = "127.0.0.1:$registryPort"
            val repo = "test/${UUID.randomUUID()}"
            val (_, imageDigest) = testCanBuildMinimalImage("tinyJibPublish", "$registry/$repo:latest", multiplatform)

            when (val manifest = fetchManifest(registry, repo, imageDigest)) {
                is ImageManifest.DockerMultiplatformList -> {
                    // TODO verify manifests (multiple if mutliplatform == true!)
                }
                is ImageManifest.Oci -> {
                    val config = fetchConfig(registry, repo, manifest)
                    assertEquals("java", config.config.entrypoint.first())
                }
            }

        } finally {
            container.stop()
        }
    }

    @Test
    fun canPublishMinimalOciImage() {
        testImagePublish(multiplatform = false)
    }

    @Test
    fun canPublishMinimalDockerImage() {
        testImagePublish(multiplatform = true)
    }
}
