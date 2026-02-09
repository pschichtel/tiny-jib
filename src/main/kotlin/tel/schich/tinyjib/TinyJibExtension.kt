package tel.schich.tinyjib

import tel.schich.tinyjib.params.BaseImageParameters
import tel.schich.tinyjib.params.ContainerParameters
import tel.schich.tinyjib.params.DockerClientParameters
import tel.schich.tinyjib.params.ExtraDirectoriesParameters
import tel.schich.tinyjib.params.OutputPathsParameters
import tel.schich.tinyjib.params.TargetImageParameters
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.newInstance

const val DEFAULT_ALLOW_INSECURE_REGISTRIES: Boolean = false

private fun getSourceSet(project: Project, name: String): Provider<SourceSet> {
    return project.provider { project.extensions.getByType(SourceSetContainer::class.java).getByName(name) }
}

class TinyJibExtension(project: Project) {
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

    val allowInsecureRegistries: Property<Boolean> = project.objects.property(Boolean::class.java)
    val configurationName: Property<String> = project.objects.property(String::class.java)
    val sourceSet: Property<SourceSet> = project.objects.property(SourceSet::class.java)

    init {
        allowInsecureRegistries.convention(DEFAULT_ALLOW_INSECURE_REGISTRIES)
        configurationName.convention(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        sourceSet.convention(getSourceSet(project, "main"))
    }
}