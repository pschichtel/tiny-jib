package tel.schich.tinyjib.jib

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.JibContainer
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer
import com.google.cloud.tools.jib.api.buildplan.FilePermissions
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.api.buildplan.ModificationTimeProvider
import com.google.cloud.tools.jib.filesystem.DirectoryWalker
import com.google.cloud.tools.jib.filesystem.PathConsumer
import org.gradle.api.Task
import tel.schich.tinyjib.TinyJibExtension
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Predicate

private val objectMapper = JsonMapper()

private data class ImageMetadataOutput(
    val image: String,
    val imageId: String,
    val imageDigest: String,
    val tags: List<String>,
    val imagePushed: Boolean,
)

private fun buildImage(jibContainerBuilder: JibContainerBuilder, containerizer: Containerizer, metadataOutputPath: Path): JibContainer {
    val jibContainer = jibContainerBuilder.containerize(containerizer)


    val imageDigest = jibContainer.digest.toString()
    Files.write(metadataOutputPath.resolve("tiny-jib.digest"), imageDigest.encodeToByteArray())

    val imageId = jibContainer.imageId.toString()
    Files.write(metadataOutputPath.resolve("tiny-jib.id"), imageId.encodeToByteArray())

    val metadataOutput = ImageMetadataOutput(
        image = jibContainer.targetImage.toString(),
        imageId = jibContainer.imageId.toString(),
        imageDigest = jibContainer.digest.toString(),
        tags = jibContainer.tags.map { it.toString() },
        imagePushed = jibContainer.isImagePushed,
    )
    objectMapper.writeValue(metadataOutputPath.resolve("tiny-jib.json").toFile(), metadataOutput)

    return jibContainer
}

fun publish(builder: JibContainerBuilder, outputPath: Path): JibContainer {
    val builder = builder.setFormat(ImageFormat.OCI)
    val targetImage = RegistryImage.named("???")
    val containerizer = Containerizer.to(targetImage)
    return buildImage(builder, containerizer, outputPath)
}

fun buildForDocker(builder: JibContainerBuilder, outputPath: Path): JibContainer {
    val builder = builder.setFormat(ImageFormat.Docker)
    val targetImage = DockerDaemonImage.named("???")
    targetImage.setDockerEnvironment(emptyMap())
    val containerizer = Containerizer.to(targetImage)

    return buildImage(builder, containerizer, outputPath)
}

fun buildTar(builder: JibContainerBuilder, outputPath: Path): JibContainer {
    val builder = builder.setFormat(ImageFormat.OCI)
    val targetImage = TarImage.at(outputPath.resolve("image.tar"))
    val containerizer = Containerizer.to(targetImage)
    return buildImage(builder, containerizer, outputPath)
}

fun Task.adaptLogs(): Consumer<LogEvent> {
    return Consumer {
        when (it.level) {
            LogEvent.Level.ERROR -> logger.error(it.message)
            LogEvent.Level.WARN -> logger.warn(it.message)
            LogEvent.Level.LIFECYCLE -> logger.lifecycle(it.message)
            LogEvent.Level.PROGRESS -> logger.lifecycle(it.message)
            LogEvent.Level.INFO -> logger.info(it.message)
            LogEvent.Level.DEBUG -> logger.debug(it.message)
        }
    }
}

private val creationTimeFormatter = DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ISO_DATE_TIME) // parses isoStrict
    // add ability to parse with no ":" in tz
    .optionalStart()
    .appendOffset("+HHmm", "+0000")
    .optionalEnd()
    .toFormatter()

fun parseCreationTime(time: String?): Instant = when (time) {
    null, "EPOCH" -> Instant.EPOCH
    "USE_CURRENT_TIMESTAMP" -> Instant.now()
    else -> creationTimeFormatter.parse(time, Instant::from)
}
