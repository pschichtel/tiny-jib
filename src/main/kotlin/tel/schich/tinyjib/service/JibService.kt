package tel.schich.tinyjib.service

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.LogEvent
import com.google.cloud.tools.jib.api.Ports
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.specs.Spec
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibPlugin
import tel.schich.tinyjib.getPlatforms
import tel.schich.tinyjib.jib.ImageMetadataOutput
import tel.schich.tinyjib.jib.SimpleModificationTimeProvider
import tel.schich.tinyjib.jib.addConfigBasedRetrievers
import tel.schich.tinyjib.jib.configureEntrypoint
import tel.schich.tinyjib.jib.configureExtraDirectoryLayers
import tel.schich.tinyjib.jib.getCredentials
import tel.schich.tinyjib.params.ImageParams
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.function.Consumer
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.sequences.map
import kotlin.text.split
import kotlin.text.startsWith

abstract class JibService : BuildService<BuildServiceParameters.None> {
    private val logger: Logger = Logging.getLogger(javaClass)

    private val logAdapter = Consumer<LogEvent> {
        when (it.level) {
            LogEvent.Level.ERROR -> logger.error(it.message)
            LogEvent.Level.WARN -> logger.warn(it.message)
            null, LogEvent.Level.LIFECYCLE -> logger.lifecycle(it.message)
            LogEvent.Level.PROGRESS -> logger.lifecycle(it.message)
            LogEvent.Level.INFO -> logger.info(it.message)
            LogEvent.Level.DEBUG -> logger.debug(it.message)
        }
    }

    private val creationTimeFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_DATE_TIME) // parses isoStrict
        // add ability to parse with no ":" in tz
        .optionalStart()
        .appendOffset("+HHmm", "+0000")
        .optionalEnd()
        .toFormatter()

    private fun parseCreationTime(time: String?): Instant = when (time) {
        null, "EPOCH" -> Instant.EPOCH
        "USE_CURRENT_TIMESTAMP" -> Instant.now()
        else -> creationTimeFormatter.parse(time, Instant::from)
    }

    internal fun configureCredentialRetrievers(imageRef: ImageReference, image: RegistryImage, imageParams: ImageParams) {
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

    private fun setupJavaBuilder(extension: TinyJibExtension): JavaContainerBuilder {
        val from = extension.from
        val imageName = from.image.get()
        if (imageName.startsWith(Jib.TAR_IMAGE_PREFIX)) {
            return JavaContainerBuilder.from(imageName)
        }

        val imageReference = ImageReference.parse(imageName.split("://", limit = 2).last())
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

    private fun JavaContainerBuilder.configureDependencies(
        sourceSetOutputClassesDir: FileCollection,
        sourceSetOutputResourcesDir: RegularFileProperty,
        configuration: FileCollection,
        projectDependencies: FileCollection,
    ): JavaContainerBuilder {

        val classesOutputDirectories = sourceSetOutputClassesDir
            .filter(Spec { obj -> obj.exists() })

        val resourcesOutputDirectory = sourceSetOutputResourcesDir.orNull?.asFile?.toPath()
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
            addResources(resourcesOutputDirectory)
        }
        for (classesOutputDirectory in classesOutputDirectories) {
            addClasses(classesOutputDirectory.toPath())
        }

        return this
    }

    protected fun setupBuilder(
        extension: TinyJibExtension,
        applicationCachePath: Path,
        sourceSetOutputClassesDir: FileCollection,
        sourceSetOutputResourcesDir: RegularFileProperty,
        configuration: FileCollection,
        projectDependencies: FileCollection,
    ): JibContainerBuilder {
        val container = extension.container
        val modificationTimeProvider =
            SimpleModificationTimeProvider(container.filesModificationTime.get())
        val appRoot = AbsoluteUnixPath.get(container.appRoot.get())

        val javaContainerBuilder = setupJavaBuilder(extension)
            .setAppRoot(appRoot)
            .setModificationTimeProvider(modificationTimeProvider)
            .configureDependencies(
                sourceSetOutputClassesDir,
                sourceSetOutputResourcesDir,
                configuration,
                projectDependencies,
            )

        val platforms = getPlatforms(extension)
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
                applicationCachePath,
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

    fun buildImage(
        extension: TinyJibExtension,
        containerizer: Containerizer,
        forDocker: Boolean,
        baseImageCachePath: Path,
        applicationCachePath: Path,
        sourceSetOutputClassesDir: FileCollection,
        sourceSetOutputResourcesDir: RegularFileProperty,
        configuration: FileCollection,
        projectDependencies: FileCollection,
        offlineMode: Boolean,
    ) {
        val jibContainerBuilder = setupBuilder(
            extension,
            applicationCachePath,
            sourceSetOutputClassesDir,
            sourceSetOutputResourcesDir,
            configuration,
            projectDependencies,
        )
        if (forDocker) {
            jibContainerBuilder.setFormat(ImageFormat.Docker)
        }

        val jibContainer = containerizer
            .setOfflineMode(offlineMode)
            .setToolName("tiny-jib")
            .setToolVersion(TinyJibPlugin::class.java.`package`.implementationVersion)
            .setAllowInsecureRegistries(extension.allowInsecureRegistries.get())
            .setBaseImageLayersCache(baseImageCachePath)
            .setApplicationLayersCache(applicationCachePath)
            .apply { extension.to.tags.get().forEach { withAdditionalTag(it) } }
            .let(jibContainerBuilder::containerize)

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
        Files.newOutputStream(extension.outputPaths.imageJson.get().toPath()).use { file ->
            @OptIn(ExperimentalSerializationApi::class)
            Json.encodeToStream(metadataOutput, file)
        }
    }
}
