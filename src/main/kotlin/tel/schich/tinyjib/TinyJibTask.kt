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
import tel.schich.tinyjib.params.AuthParameters
import tel.schich.tinyjib.params.CredHelperParameters
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.property
import tel.schich.tinyjib.jib.SimpleModificationTimeProvider
import tel.schich.tinyjib.jib.addConfigBasedRetrievers
import tel.schich.tinyjib.jib.configureEntrypoint
import tel.schich.tinyjib.jib.configureExtraDirectoryLayers
import tel.schich.tinyjib.jib.getCredentials
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.function.Consumer
import kotlin.collections.orEmpty

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

const val OUTPUT_FILE_NAME = "tiny-jib"
const val OUTPUT_DIRECTORY_NAME = "tiny-jib"
const val CACHE_DIRECTORY_NAME = "$OUTPUT_DIRECTORY_NAME-cache"

@CacheableTask
abstract class TinyJibTask(val extension: TinyJibExtension) : DefaultTask() {
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
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    val cacheDir: DirectoryProperty = project.objects.directoryProperty()

    @Input
    val offlineMode: Property<Boolean> = project.objects.property()
    @Input
    val configurationName: Property<String> = project.objects.property()
    @Input
    val sourceSet: Property<SourceSet> = project.objects.property()

    init {
        outputDir.convention(project.layout.buildDirectory.dir(OUTPUT_DIRECTORY_NAME))
        cacheDir.convention(project.layout.buildDirectory.dir(CACHE_DIRECTORY_NAME))
        offlineMode.convention(project.gradle.startParameter.isOffline)
        configurationName.convention(extension.configurationName)
        sourceSet.convention(extension.sourceSet)
    }

    private fun setupJavaBuilder(): JavaContainerBuilder {
        val from = extension.getFrom()
        val imageName = from.image.get()
        if (imageName.startsWith(Jib.TAR_IMAGE_PREFIX)) {
            return JavaContainerBuilder.from(imageName)
        }

        val imageReference = ImageReference.parse(imageName.split("://", limit = 2).last());
        if (imageName.startsWith(Jib.DOCKER_DAEMON_IMAGE_PREFIX)) {
            val image = DockerDaemonImage.named(imageReference)
                .setDockerEnvironment(extension.getDockerClient().environment)
            extension.getDockerClient().executable?.let {
                image.setDockerExecutable(Paths.get(it))
            }
            return JavaContainerBuilder.from(image)
        }

        val credHelper = from.credHelper
        val baseImage = RegistryImage.named(imageReference)
        configureCredentialRetrievers(imageReference, baseImage, from.auth, credHelper)

        val credHelperFactory = CredentialRetrieverFactory.forImage(imageReference, logAdapter, credHelper.environment.get())
        getCredentials(from.auth)?.let {
            credHelperFactory.known(it, "from")
        }

        return JavaContainerBuilder.from(baseImage)
    }

    private fun JavaContainerBuilder.configureDependencies(): JavaContainerBuilder {
        val configurationName = configurationName.get()
        val sourceSet: SourceSet = sourceSet.get()

        // TODO this is not Configuration Cache safe

        val projectDependencies: FileCollection = project.configurations.getByName(configurationName)
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
        val allFiles =
            project.configurations.getByName(configurationName)
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

        val from = extension.getFrom()
        val container = extension.getContainer()
        val modificationTimeProvider =
            SimpleModificationTimeProvider(container.filesModificationTime.get())
        val appRoot = container.getAppRoot()
            ?.ifEmpty { null }
            ?: JavaContainerBuilder.DEFAULT_APP_ROOT

        val javaContainerBuilder = setupJavaBuilder()
            .setAppRoot(appRoot)
            .setModificationTimeProvider(modificationTimeProvider)
            .configureDependencies()

        val platforms = from.platforms.get().orEmpty().mapNotNull {
            val architecture = it.architecture.orNull ?: return@mapNotNull null
            val os = it.os.orNull ?: return@mapNotNull null
            Platform(architecture, os)
        }.toSet()
        val volumes = container.volumes.orEmpty().map {
            AbsoluteUnixPath.get(it)
        }.toSet()

        val dependencies = project.configurations.getByName(configurationName.get())
            .asSequence()
            .filter { it.exists() && it.isFile() && it.getName().lowercase().endsWith(".jar") }
            .map { it.toPath() }
            .toList()

        return javaContainerBuilder.toContainerBuilder().apply {
            setFormat(ImageFormat.OCI)
            setPlatforms(platforms)
            configureEntrypoint(
                cacheDir.asFile.get().toPath(),
                appRoot,
                container.entrypoint?.ifEmpty { null },
                container.mainClass!!,
                container.jvmFlags,
                dependencies,
                container.extraClasspath,
            )
            setProgramArguments(container.args?.ifEmpty { null })
            setEnvironment(container.environment)
            setExposedPorts(Ports.parse(container.getPorts()))
            setVolumes(volumes)
            setLabels(container.labels.get())
            setUser(container.user)
            setCreationTime(parseCreationTime(container.creationTime.get()))
            setWorkingDirectory(container.workingDirectory?.let { AbsoluteUnixPath.get(it) })

            configureExtraDirectoryLayers(extension, modificationTimeProvider)
        }
    }

    protected fun buildImage(jibContainerBuilder: JibContainerBuilder, containerizer: Containerizer, metadataOutputPath: Path, cachePath: Path): JibContainer {
        val containerizer = containerizer.setOfflineMode(offlineMode.get())
            .setToolName("tiny-jib")
            .setToolVersion(TinyJibPlugin::class.java.`package`.implementationVersion)
            .setAllowInsecureRegistries(extension.allowInsecureRegistries.get())
            .setBaseImageLayersCache(cachePath)
            .setApplicationLayersCache(cachePath)
        val jibContainer = jibContainerBuilder.containerize(containerizer)

        val imageDigest = jibContainer.digest.toString()
        Files.write(metadataOutputPath.resolve("$OUTPUT_FILE_NAME.digest"), imageDigest.encodeToByteArray())

        val imageId = jibContainer.imageId.toString()
        Files.write(metadataOutputPath.resolve("$OUTPUT_FILE_NAME.id"), imageId.encodeToByteArray())

        val metadataOutput = ImageMetadataOutput(
            image = jibContainer.targetImage.toString(),
            imageId = jibContainer.imageId.toString(),
            imageDigest = jibContainer.digest.toString(),
            tags = jibContainer.tags.map { it.toString() },
            imagePushed = jibContainer.isImagePushed,
        )
        objectMapper.writeValue(metadataOutputPath.resolve("$OUTPUT_FILE_NAME.json").toFile(), metadataOutput)

        return jibContainer
    }

    private fun configureCredentialRetrievers(imageRef: ImageReference, image: RegistryImage, authParams: AuthParameters?, credHelperParams: CredHelperParameters) {
        val credHelperEnv = credHelperParams.environment.orNull.orEmpty()
        val credHelperFactory = CredentialRetrieverFactory.forImage(imageRef, logAdapter, credHelperEnv)
        if (authParams != null) {
            getCredentials(authParams)?.let {
                image.addCredentialRetriever(credHelperFactory.known(it, "tiny-jib"))
            }
        }
        credHelperParams.helper.orNull?.let { helperName ->
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
        val to = extension.getTo()
        return ImageReference.parse(to.image.get() + ":" + to.tags.get().first())
    }
}