package tel.schich.tinyjib

import com.google.cloud.tools.jib.api.DockerDaemonImage
import com.google.cloud.tools.jib.api.ImageReference
import com.google.cloud.tools.jib.api.JavaContainerBuilder
import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.api.JibContainerBuilder
import com.google.cloud.tools.jib.api.Ports
import com.google.cloud.tools.jib.api.RegistryImage
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath
import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.api.buildplan.Platform
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import tel.schich.tinyjib.jib.SimpleModificationTimeProvider
import tel.schich.tinyjib.jib.adaptLogs
import tel.schich.tinyjib.jib.configureCredentialRetrievers
import tel.schich.tinyjib.jib.configureEntrypoint
import tel.schich.tinyjib.jib.configureExtraDirectoryLayers
import tel.schich.tinyjib.jib.getCredentials
import tel.schich.tinyjib.jib.parseCreationTime
import java.nio.file.Files
import java.nio.file.Paths

const val CACHE_DIRECTORY_NAME = "tiny-jib-cache"

@CacheableTask
abstract class TinyJibTask(val extension: TinyJibExtension) : DefaultTask() {

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    val cacheDir: DirectoryProperty = project.objects.directoryProperty()

    @Input
    val configurationName: Property<String> = project.objects.property(String::class.java)
    @Input
    val sourceSet: Property<SourceSet> = project.objects.property(SourceSet::class.java)

    init {
        outputDir.convention(project.layout.buildDirectory.dir("tiny-jib"))
        cacheDir.convention(project.layout.buildDirectory.dir("tiny-jib-cache"))
        sourceSet.convention(getSourceSet("main"))
    }

    private fun setupJavaBuilder(): JavaContainerBuilder {
        val from = extension.getFrom()
        val imageName = from.image!!
        if (imageName.startsWith(Jib.TAR_IMAGE_PREFIX)) {
            return JavaContainerBuilder.from(imageName)
        }

        val imageReference = ImageReference.parse(imageName.split("://", limit = 2).last());
        if (imageName.startsWith(Jib.DOCKER_DAEMON_IMAGE_PREFIX)) {
            val image = DockerDaemonImage.named(imageReference)
                .setDockerEnvironment(extension.getDockerClient().getEnvironment())
            extension.getDockerClient().executable?.let {
                image.setDockerExecutable(Paths.get(it))
            }
            return JavaContainerBuilder.from(image)
        }

        val credHelper = from.credHelper
        val baseImage = RegistryImage.named(imageReference)
        configureCredentialRetrievers(imageReference, baseImage, from.auth, credHelper)

        val credHelperFactory = CredentialRetrieverFactory.forImage(imageReference, adaptLogs(), credHelper?.environment)
        getCredentials(from.auth)?.let {
            credHelperFactory.known(it, from.auth.authDescriptor)
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
            val architecture = it.architecture ?: return@mapNotNull null
            val os = it.os ?: return@mapNotNull null
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
}

private fun Task.getSourceSet(name: String): Provider<SourceSet> {
    return project.provider { project.extensions.getByType(SourceSetContainer::class.java).getByName(name) }
}