package tel.schich.tinyjib.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class BlobRef(
    val mediaType: String,
    val digest: String,
    val size: UInt,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("mediaType")
@Serializable
sealed interface ImageManifest {
    /**
     * See: https://github.com/opencontainers/image-spec/blob/main/manifest.md
     */
    @SerialName("application/vnd.oci.image.manifest.v1+json")
    @Serializable
    data class Oci(
        val schemaVersion: Int,
        val config: BlobRef,
        val layers: List<BlobRef>,
    ) : ImageManifest

    @SerialName("application/vnd.docker.distribution.manifest.list.v2+json")
    @Serializable
    data class DockerMultiplatformList(
        val schemaVersion: Int,
        val manifests: List<BlobRef>,
    ) : ImageManifest
}

/**
 * See: https://github.com/opencontainers/image-spec/blob/main/config.md
 */
@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class ImageConfig(
    val config: ImageConfigConfig,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class ImageConfigConfig(
    @SerialName("Env")
    val env: List<String> = emptyList(),
    @SerialName("Entrypoint")
    val entrypoint: List<String> = emptyList(),
    @SerialName("Cmd")
    val cmd: List<String> = emptyList(),
)

private inline fun <reified T : Any> fetchObject(registry: String, repo: String, kind: String, digest: String, mediaType: String): T {
    val uri = URL("http://$registry/v2/$repo/$kind/$digest")
    val connection = uri.openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", mediaType)

    connection.doInput = true
    connection.doOutput = false

    val responseText = connection.inputStream.use {
        it.readBytes().decodeToString()
    }
    assertEquals(200, connection.responseCode)

    return Json.decodeFromString<T>(responseText)
}

fun fetchManifest(registry: String, imageRepo: String, imageDigest: String): ImageManifest {
    return fetchObject(
        registry,
        imageRepo,
        kind = "manifests",
        imageDigest,
        mediaType = "application/vnd.oci.image.manifest.v1+json",
    )
}

fun fetchConfig(registry: String, imageRepo: String, manifest: ImageManifest.Oci): ImageConfig {
    return fetchObject(
        registry,
        imageRepo,
        kind = "blobs",
        manifest.config.digest,
        manifest.config.mediaType,
    )
}
