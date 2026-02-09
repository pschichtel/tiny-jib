package tel.schich.tinyjib

import tel.schich.tinyjib.params.BaseImageParameters
import com.google.cloud.tools.jib.gradle.ContainerParameters
import com.google.cloud.tools.jib.gradle.DockerClientParameters
import com.google.cloud.tools.jib.gradle.ExtraDirectoriesParameters
import com.google.cloud.tools.jib.gradle.OutputPathsParameters
import tel.schich.tinyjib.params.TargetImageParameters
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

const val DEFAULT_ALLOW_INSECURE_REGISTIRIES: Boolean = false

private fun getSourceSet(project: Project, name: String): Provider<SourceSet> {
    return project.provider { project.extensions.getByType(SourceSetContainer::class.java).getByName(name) }
}

class TinyJibExtension(project: Project) {
    private val from: BaseImageParameters = project.objects.newInstance(BaseImageParameters::class.java)
    private val to: TargetImageParameters = project.objects.newInstance(TargetImageParameters::class.java)
    private val container: ContainerParameters = project.objects.newInstance(ContainerParameters::class.java)
    private val extraDirectories: ExtraDirectoriesParameters = project.objects.newInstance(ExtraDirectoriesParameters::class.java, project)
    private val dockerClient: DockerClientParameters = project.objects.newInstance(DockerClientParameters::class.java)
    private val outputPaths: OutputPathsParameters = project.objects.newInstance(OutputPathsParameters::class.java, project)

    val allowInsecureRegistries: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(DEFAULT_ALLOW_INSECURE_REGISTIRIES)
    val configurationName: Property<String> = project.objects.property(String::class.java)
        .convention(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
    val sourceSet: Property<SourceSet> = project.objects.property(SourceSet::class.java)
        .convention(getSourceSet(project, "main"))

    fun from(action: Action<BaseImageParameters>) {
        action.execute(from)
    }

    fun to(action: Action<TargetImageParameters>) {
        action.execute(to)
    }

    fun container(action: Action<ContainerParameters>) {
        action.execute(container)
    }

    fun extraDirectories(action: Action<ExtraDirectoriesParameters>) {
        action.execute(extraDirectories)
    }

    fun dockerClient(action: Action<DockerClientParameters>) {
        action.execute(dockerClient)
    }

    fun outputPaths(action: Action<OutputPathsParameters>) {
        action.execute(outputPaths)
    }

    @Nested
    @Optional
    fun getFrom(): BaseImageParameters {
        return from
    }

    @Nested
    @Optional
    fun getTo(): TargetImageParameters {
        return to
    }

    @Nested
    @Optional
    fun getContainer(): ContainerParameters {
        return container
    }

    @Nested
    @Optional
    fun getExtraDirectories(): ExtraDirectoriesParameters {
        return extraDirectories
    }

    @Nested
    @Optional
    fun getDockerClient(): DockerClientParameters {
        return dockerClient
    }

    @Nested
    @Optional
    fun getOutputPaths(): OutputPathsParameters {
        return outputPaths
    }
}