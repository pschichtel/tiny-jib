package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.TarImage
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibPlugin
import tel.schich.tinyjib.getPlatforms
import javax.inject.Inject

@CacheableTask
class TinyJIbDownloadBaseImageTask @Inject constructor(private val extension: TinyJibExtension) : DefaultTask() {
    @Input
    val imageName: Property<String> = project.objects.property()

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    fun download() {
        val imagePath = temporaryDir.toPath().resolve("image.tar")
        val appLayersCachePath = temporaryDir.toPath().resolve("image.tar")
        val targetImage = TarImage.at(imagePath)
        val containerizer = Containerizer.to(targetImage)
            .setOfflineMode(false)
            .setToolName("tiny-jib")
            .setToolVersion(TinyJibPlugin::class.java.`package`.implementationVersion)
            .setAllowInsecureRegistries(extension.allowInsecureRegistries.get())
            .setBaseImageLayersCache(outputDir.asFile.get().toPath())
            .setApplicationLayersCache(appLayersCachePath)
        val platforms = getPlatforms(extension)

        val imageReference = ImageReference.parse(imageName.get())
        val baseImage = RegistryImage.named(imageReference)
        JavaContainerBuilder.from(baseImage)
            .toContainerBuilder()
            .setFormat(ImageFormat.OCI)
            .apply {
                if (platforms.isNotEmpty()) {
                    setPlatforms(platforms)
                }
            }
            .containerize(containerizer)
    }

}