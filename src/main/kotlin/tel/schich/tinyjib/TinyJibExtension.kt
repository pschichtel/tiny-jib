package tel.schich.tinyjib

import tel.schich.tinyjib.params.BaseImageParameters
import tel.schich.tinyjib.params.ContainerParameters
import tel.schich.tinyjib.params.DockerClientParameters
import tel.schich.tinyjib.params.ExtraDirectoriesParameters
import tel.schich.tinyjib.params.OutputPathsParameters
import tel.schich.tinyjib.params.TargetImageParameters
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

const val DEFAULT_ALLOW_INSECURE_REGISTRIES: Boolean = false

abstract class TinyJibExtension(project: Project) {
    @Nested
    val from: BaseImageParameters = project.objects.newInstance(BaseImageParameters::class.java)
    @Nested
    val to: TargetImageParameters = project.objects.newInstance(TargetImageParameters::class.java)
    @Nested
    val container: ContainerParameters = project.objects.newInstance(ContainerParameters::class.java)
    @Nested
    val extraDirectories: ExtraDirectoriesParameters = project.objects.newInstance(ExtraDirectoriesParameters::class.java)
    @Nested
    val dockerClient: DockerClientParameters = project.objects.newInstance(DockerClientParameters::class.java)
    @Nested
    val outputPaths: OutputPathsParameters = project.objects.newInstance(OutputPathsParameters::class.java, project)

    @get:Input
    abstract val allowInsecureRegistries: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val configurationName: Property<String>

    @get:Input
    abstract val sourceSetName: Property<String>

    @get:LocalState
    abstract val applicationCache: DirectoryProperty

    @get:LocalState
    abstract val baseImageCache: DirectoryProperty

    init {
        allowInsecureRegistries.convention(DEFAULT_ALLOW_INSECURE_REGISTRIES)
        sourceSetName.convention("main")
    }

    fun from(block: BaseImageParameters.() -> Unit): Unit = from.block()
    fun to(block: TargetImageParameters.() -> Unit): Unit = to.block()
    fun container(block: ContainerParameters.() -> Unit): Unit = container.block()
    fun extraDirectories(block: ExtraDirectoriesParameters.() -> Unit): Unit = extraDirectories.block()
    fun dockerClient(block: DockerClientParameters.() -> Unit): Unit = dockerClient.block()
    fun outputPaths(block: OutputPathsParameters.() -> Unit): Unit = outputPaths.block()
}
