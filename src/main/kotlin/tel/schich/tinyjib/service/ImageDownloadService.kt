package tel.schich.tinyjib.service

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibPlugin
import tel.schich.tinyjib.getPlatforms
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ImageDownloadParams : BuildServiceParameters {
    val outputDir: DirectoryProperty
}

private class ImageDownloadResult(private val imageName: String, private val logger: Logger) {
    private val lock = ReentrantLock()
    private var outputPath: Path? = null

    fun fetch(block: () -> Path): Path {
        lock.withLock {
            val outputPath = this.outputPath
            if (outputPath == null) {
                val file = block()
                this.outputPath = file
                return file
            }
            logger.lifecycle("Download skipped for $imageName, already downloaded!")
            return outputPath
        }
    }
}

abstract class ImageDownloadService : BuildService<ImageDownloadParams> {

    private val downloads = ConcurrentHashMap<String, ImageDownloadResult>()
    private val logger: Logger = Logging.getLogger(javaClass)

    private fun deriveKey(imageName: String) = imageName
        .replace("_", "__")
        .replace(":", "_")
        .replace("-", "--")
        .replace("/", "-")

    fun download(temporaryDir: File, extension: TinyJibExtension): Path {
        val imageName = extension.from.image.get()
        val imageKey = deriveKey(imageName)
        val result = downloads.computeIfAbsent(imageName) { _ -> ImageDownloadResult(imageName, logger) }

        return result.fetch {
            val tempPath = temporaryDir.toPath().resolve("tiny-jib-tmp")
            val imagePath = tempPath.resolve("image.tar")
            val appLayersCachePath = tempPath.resolve("app-layers")
            val targetImage = TarImage.at(imagePath)
                .named("dummy:latest")
            val outputPath = parameters.outputDir.asFile.get().toPath().resolve(imageKey)
            logger.lifecycle("Downloading $imageName to $outputPath")
            val containerizer = Containerizer.to(targetImage)
                .setOfflineMode(false)
                .setToolName("tiny-jib")
                .setToolVersion(TinyJibPlugin::class.java.`package`.implementationVersion)
                .setAllowInsecureRegistries(extension.allowInsecureRegistries.get())
                .setBaseImageLayersCache(outputPath)
                .setApplicationLayersCache(appLayersCachePath)
            val platforms = getPlatforms(extension)

            val imageReference = ImageReference.parse(imageName)
            val baseImage = RegistryImage.named(imageReference)
            Jib.from(baseImage)
                .setFormat(ImageFormat.OCI)
                .apply {
                    if (platforms.isNotEmpty()) {
                        setPlatforms(platforms)
                    }
                }
                .containerize(containerizer)

            Files.walk(tempPath).use { walk ->
                walk.sorted(Comparator.reverseOrder())
                    .forEach(Files::delete)
            }

            outputPath
        }
    }
}