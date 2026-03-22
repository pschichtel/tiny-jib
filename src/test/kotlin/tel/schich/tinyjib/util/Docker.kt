package tel.schich.tinyjib.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class DockerImageConfig(
    @SerialName("Env")
    val env: List<String> = emptyList(),
    @SerialName("Cmd")
    val cmd: List<String> = emptyList(),
    @SerialName("WorkingDir")
    val workingDir: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class DockerImage(
    @SerialName("Id")
    val id: String,
    @SerialName("RepoTags")
    val repoTags: List<String>,
    @SerialName("RepoDigests")
    val repoDigests: List<String>,
    @SerialName("Config")
    val config: DockerImageConfig,
)

fun inspectDockerImage(imageName: String): DockerImage {
    val result = runCommand(listOf("docker", "image", "inspect", imageName), 5.seconds)
    assertEquals(0, result.exitCode)
    assertEquals("", result.stderr)
    return Json.decodeFromString<List<DockerImage>>(result.stdout).first()
}

fun deleteDockerImage(imageName: String) {
    val result = runCommand(listOf("docker", "image", "rm", imageName), 5.seconds)
    assertEquals(0, result.exitCode)
}
