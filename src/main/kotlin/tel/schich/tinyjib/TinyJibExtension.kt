package tel.schich.tinyjib

import tel.schich.tinyjib.params.BaseImageParameters
import tel.schich.tinyjib.params.ContainerParameters
import tel.schich.tinyjib.params.DockerClientParameters
import tel.schich.tinyjib.params.ExtraDirectoriesParameters
import tel.schich.tinyjib.params.OutputPathsParameters
import tel.schich.tinyjib.params.TargetImageParameters
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property

const val DEFAULT_ALLOW_INSECURE_REGISTRIES: Boolean = false

abstract class TinyJibExtension(project: Project) {
    @Nested
    val from: BaseImageParameters = project.objects.newInstance()
    @Nested
    val to: TargetImageParameters = project.objects.newInstance()
    @Nested
    val container: ContainerParameters = project.objects.newInstance()
    @Nested
    val extraDirectories: ExtraDirectoriesParameters = project.objects.newInstance()
    @Nested
    val dockerClient: DockerClientParameters = project.objects.newInstance()
    @Nested
    val outputPaths: OutputPathsParameters = project.objects.newInstance(project)

    @Input
    val allowInsecureRegistries: Property<Boolean> = project.objects.property()

    @Input
    @Optional
    val configurationName: Property<String> = project.objects.property()

    @Input
    val sourceSetName: Property<String> = project.objects.property()

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