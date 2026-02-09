package tel.schich.tinyjib

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.JibContainer
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.Ports
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.api.buildplan.Platform
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.property
import tel.schich.tinyjib.jib.SimpleModificationTimeProvider
import tel.schich.tinyjib.jib.addConfigBasedRetrievers
import tel.schich.tinyjib.jib.configureEntrypoint
import tel.schich.tinyjib.jib.configureExtraDirectoryLayers
import tel.schich.tinyjib.jib.getCredentials
import tel.schich.tinyjib.params.ImageParams
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.function.Consumer
import kotlin.Boolean
import kotlin.String
import kotlin.apply
import kotlin.collections.List
import kotlin.collections.asSequence
import kotlin.collections.filterIsInstance
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.orEmpty
import kotlin.collections.toSet
import kotlin.let
import kotlin.sequences.filter
import kotlin.sequences.map
import kotlin.sequences.toList
import kotlin.text.contains
import kotlin.text.encodeToByteArray
import kotlin.text.endsWith
import kotlin.text.lowercase

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

private val objectMapper = JsonMapper()

private data class ImageMetadataOutput(
    val image: String,
    val imageId: String,
    val imageDigest: String,
    val tags: List<String>,
    val imagePushed: Boolean,
)

const val OUTPUT_DIRECTORY_NAME = "tiny-jib"
const val CACHE_DIRECTORY_NAME = "$OUTPUT_DIRECTORY_NAME-cache"

@CacheableTask
abstract class TinyJibTask(@Nested val extension: TinyJibExtension) : DefaultTask() {
    private val logAdapter = Consumer<LogEvent> {
        when (it.level) {
            LogEvent.Level.ERROR -> logger.error(it.message)
            LogEvent.Level.WARN -> logger.warn(it.message)
            LogEvent.Level.LIFECYCLE -> logger.lifecycle(it.message)
            LogEvent.Level.PROGRESS -> logger.lifecycle(it.message)
            LogEvent.Level.INFO -> logger.info(it.message)
            LogEvent.Level.DEBUG -> logger.debug(it.message)
        }
    }

    @OutputDirectory
    val cacheDir: DirectoryProperty = project.objects.directoryProperty()

    @Input
    val offlineMode: Property<Boolean> = project.objects.property()

    init {
        cacheDir.convention(project.layout.buildDirectory.dir(CACHE_DIRECTORY_NAME))
        offlineMode.convention(project.gradle.startParameter.isOffline)
    }

    private fun setupJavaBuilder(): JavaContainerBuilder {
        val from = extension.from
        val imageName = from.image.get()
        if (imageName.startsWith(Jib.TAR_IMAGE_PREFIX)) {
            return JavaContainerBuilder.from(imageName)
        }

        val imageReference = ImageReference.parse(imageName.split("://", limit = 2).last());
        if (imageName.startsWith(Jib.DOCKER_DAEMON_IMAGE_PREFIX)) {
            val image = DockerDaemonImage.named(imageReference)
                .setDockerEnvironment(extension.dockerClient.environment.orNull.orEmpty())
            extension.dockerClient.executable.orNull?.let {
                image.setDockerExecutable(Paths.get(it))
            }
            return JavaContainerBuilder.from(image)
        }

        val credHelper = from.credHelper
        val baseImage = RegistryImage.named(imageReference)
        configureCredentialRetrievers(imageReference, baseImage, from)

        val credHelperFactory = CredentialRetrieverFactory.forImage(imageReference, logAdapter, credHelper.environment.get())
        getCredentials(from.auth)?.let {
            credHelperFactory.known(it, "from")
        }

        return JavaContainerBuilder.from(baseImage)
    }

    private fun JavaContainerBuilder.configureDependencies(sourceSet: SourceSet, configuration: Configuration): JavaContainerBuilder {
        val projectDependencies: FileCollection = configuration
                .resolvedConfiguration
                .resolvedArtifacts
                .filterIsInstance<ResolvedArtifact>()
                .asSequence()
                .filter {
                    it.id.componentIdentifier is ProjectComponentIdentifier
                }
                .map { it.file }
                .toList()
                .let { project.files(it) }

        val classesOutputDirectories = sourceSet.output.classesDirs
            .filter(Spec { obj -> obj.exists() })

        val resourcesOutputDirectory = sourceSet.output.resourcesDir?.toPath()
        val allFiles = configuration
                .filter(Spec { obj -> obj.exists() })

        val nonProjectDependencies = allFiles
                .minus(classesOutputDirectories)
                .minus(projectDependencies)
                .filter(Spec { file -> file.toPath() != resourcesOutputDirectory })

        val snapshotDependencies = nonProjectDependencies
            .filter(Spec { file -> file.name.contains("SNAPSHOT") })

        val dependencies = nonProjectDependencies.minus(snapshotDependencies)

        addDependencies(dependencies.asSequence().map { it.toPath() }.toList())
        addSnapshotDependencies(snapshotDependencies.asSequence().map { it.toPath() }.toList())
        addProjectDependencies(projectDependencies.asSequence().map { it.toPath() }.toList())

        if (resourcesOutputDirectory != null && Files.exists(resourcesOutputDirectory)) {
            addResources(resourcesOutputDirectory);
        }
        for (classesOutputDirectory in classesOutputDirectories) {
            addClasses(classesOutputDirectory.toPath())
        }

        return this
    }

    protected fun setupBuilder(): JibContainerBuilder {

        val from = extension.from
        val container = extension.container
        val modificationTimeProvider =
            SimpleModificationTimeProvider(container.filesModificationTime.get())
        val appRoot = container.appRoot.get()

        // TODO this is not Configuration Cache safe
        val sourceSet: SourceSet = extension.sourceSetName.map {
            project.extensions.getByType(SourceSetContainer::class.java).getByName(it)
        }.get()
        val configurationName = extension.configurationName
            .getOrElse(sourceSet.runtimeClasspathConfigurationName)
        val configuration = project.configurations.getByName(configurationName)

        val javaContainerBuilder = setupJavaBuilder()
            .setAppRoot(appRoot)
            .setModificationTimeProvider(modificationTimeProvider)
            .configureDependencies(sourceSet, configuration)

        val platforms = from.platforms.get().orEmpty().mapNotNull {
            val architecture = it.architecture.orNull ?: return@mapNotNull null
            val os = it.os.orNull ?: return@mapNotNull null
            Platform(architecture, os)
        }.toSet()
        val volumes = container.volumes.orNull.orEmpty().map {
            AbsoluteUnixPath.get(it)
        }.toSet()

        val dependencies = configuration
            .asSequence()
            .filter { it.exists() && it.isFile() && it.getName().lowercase().endsWith(".jar") }
            .map { it.toPath() }
            .toList()

        return javaContainerBuilder.toContainerBuilder().apply {
            setFormat(ImageFormat.OCI)
            if (platforms.isNotEmpty()) {
                setPlatforms(platforms)
            }
            configureEntrypoint(
                cacheDir.asFile.get().toPath(),
                appRoot,
                container.entrypoint.orNull?.ifEmpty { null },
                container.mainClass.get(),
                container.jvmFlags.orNull.orEmpty(),
                dependencies,
                container.extraClasspath.orNull.orEmpty(),
            )
            container.args.orNull?.ifEmpty { null }?.let {
                setProgramArguments()
            }
            setEnvironment(container.environment.orNull.orEmpty())
            container.ports.orNull?.ifEmpty { null }?.let {
                setExposedPorts(Ports.parse(it))
            }
            setVolumes(volumes)
            setLabels(container.labels.get())
            container.user.orNull?.let {
                setUser(it)
            }
            setCreationTime(parseCreationTime(container.creationTime.get()))
            container.workingDirectory.orNull?.let {
                setWorkingDirectory(AbsoluteUnixPath.get(it))
            }

            configureExtraDirectoryLayers(extension, modificationTimeProvider)
        }
    }

    protected fun buildImage(jibContainerBuilder: JibContainerBuilder, containerizer: Containerizer): JibContainer {
        val cachePath = cacheDir.asFile.get().toPath()

        val containerizer = containerizer.setOfflineMode(offlineMode.get())
            .setToolName("tiny-jib")
            .setToolVersion(TinyJibPlugin::class.java.`package`.implementationVersion)
            .setAllowInsecureRegistries(extension.allowInsecureRegistries.get())
            .setBaseImageLayersCache(cachePath)
            .setApplicationLayersCache(cachePath)
        val jibContainer = jibContainerBuilder.containerize(containerizer)

        val imageDigest = jibContainer.digest.toString()
        Files.write(extension.outputPaths.digest.get().toPath(), imageDigest.encodeToByteArray())

        val imageId = jibContainer.imageId.toString()
        Files.write(extension.outputPaths.imageId.get().toPath(), imageId.encodeToByteArray())

        val metadataOutput = ImageMetadataOutput(
            image = jibContainer.targetImage.toString(),
            imageId = jibContainer.imageId.toString(),
            imageDigest = jibContainer.digest.toString(),
            tags = jibContainer.tags.map { it.toString() },
            imagePushed = jibContainer.isImagePushed,
        )
        objectMapper.writeValue(extension.outputPaths.imageJson.get(), metadataOutput)

        return jibContainer
    }

    protected fun configureCredentialRetrievers(imageRef: ImageReference, image: RegistryImage, imageParams: ImageParams) {
        val credHelperEnv = imageParams.credHelper.environment.orNull.orEmpty()
        val credHelperFactory = CredentialRetrieverFactory.forImage(imageRef, logAdapter, credHelperEnv)
        getCredentials(imageParams.auth)?.let {
            image.addCredentialRetriever(credHelperFactory.known(it, "tiny-jib"))
        }
        imageParams.credHelper.helper.orNull?.let { helperName ->
            val helperBinaryPath = Paths.get(helperName)
            if (Files.isExecutable(helperBinaryPath)) {
                image.addCredentialRetriever(credHelperFactory.dockerCredentialHelper(helperBinaryPath))
            } else {
                image.addCredentialRetriever(credHelperFactory.dockerCredentialHelper("docker-credential-$helperName"))
            }
        }

        addConfigBasedRetrievers(credHelperFactory, image)

        image.addCredentialRetriever(credHelperFactory.wellKnownCredentialHelpers())
        image.addCredentialRetriever(credHelperFactory.googleApplicationDefaultCredentials())
    }

    protected fun targetImageName(): ImageReference {
        return ImageReference.parse(extension.to.image.get())
    }
}