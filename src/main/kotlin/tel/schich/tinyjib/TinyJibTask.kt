package tel.schich.tinyjib

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.buildplan.Platform
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSetContainer
import tel.schich.tinyjib.service.JibService

const val OUTPUT_DIRECTORY_NAME = "tiny-jib"
const val CACHE_DIRECTORY_NAME = "$OUTPUT_DIRECTORY_NAME-cache"

internal fun getPlatforms(extension: TinyJibExtension): Set<Platform> {
    return extension.from.platforms.get().orEmpty().mapNotNull {
        val architecture = it.architecture.orNull ?: return@mapNotNull null
        val os = it.os.orNull ?: return@mapNotNull null
        Platform(architecture, os)
    }.toSet()
}

@CacheableTask
abstract class TinyJibTask(@Nested val extension: TinyJibExtension) : DefaultTask() {
    @get:ServiceReference(DOWNLOAD_SERVICE_NAME)
    protected abstract val jibService: Property<JibService>

    private val applicationCache: DirectoryProperty = project.objects.directoryProperty()
    private val baseImageCache: DirectoryProperty = project.objects.directoryProperty()
    private val sourceSetOutputClassesDir: ConfigurableFileCollection = project.objects.fileCollection()
    private val sourceSetOutputResourcesDir: RegularFileProperty = project.objects.fileProperty()
    private val configuration: ConfigurableFileCollection = project.objects.fileCollection()
    private val projectDependencies: ConfigurableFileCollection = project.objects.fileCollection()

    init {
        applicationCache.convention(extension.applicationCache.orElse(project.layout.buildDirectory.dir(CACHE_DIRECTORY_NAME)))
        baseImageCache.value(extension.baseImageCache.orElse(project.rootProject.layout.buildDirectory.dir(CACHE_DIRECTORY_NAME)))

        val sourceSet = extension.sourceSetName.map { project.extensions.getByType(SourceSetContainer::class.java).getByName(it) }
        val configuration = sourceSet.map {
            val configurationName = extension.configurationName
                .getOrElse(it.runtimeClasspathConfigurationName)
            project.configurations.getByName(configurationName)
        }

        sourceSetOutputClassesDir.from(sourceSet.map { it.output.classesDirs })
        // TODO: can this be avoided?
        @Suppress("UnsafeCallOnNullableType")
        sourceSetOutputResourcesDir.fileProvider(sourceSet.map { it.output.resourcesDir!! })
        this.configuration.from(configuration)

        projectDependencies.from(configuration.map { dependency ->
            dependency.resolvedConfiguration
                .resolvedArtifacts
                .filterIsInstance<ResolvedArtifact>()
                .asSequence()
                .filter {
                    it.id.componentIdentifier is ProjectComponentIdentifier
                }
                .map { it.file }
                .toList()
        })

        inputs.files(sourceSet.map { it.runtimeClasspath })
        inputs.files(sourceSet.map { it.output })
    }

    protected fun buildImage(containerizer: Containerizer, forDocker: Boolean, offlineMode: Boolean) {
        val baseImageCachePath = baseImageCache.asFile.get().toPath()
        val applicationCachePath = applicationCache.asFile.get().toPath()

        jibService.get().buildImage(
            extension,
            containerizer,
            forDocker,
            baseImageCachePath,
            applicationCachePath,
            sourceSetOutputClassesDir,
            sourceSetOutputResourcesDir,
            configuration,
            projectDependencies,
            offlineMode,
        )
    }

    protected fun targetImageName(): ImageReference {
        return ImageReference.parse(extension.to.image.get())
    }
}
