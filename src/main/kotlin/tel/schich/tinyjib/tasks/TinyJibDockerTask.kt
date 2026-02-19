package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask
import javax.inject.Inject

abstract class TinyJibDockerTask @Inject constructor(extension: TinyJibExtension) : TinyJibTask(extension) {
    @Input
    val offlineMode: Property<Boolean> = project.objects.property(Boolean::class.java)

    init {
        offlineMode.convention(project.gradle.startParameter.isOffline)
    }

    @TaskAction
    fun performAction() {
        val targetImage = DockerDaemonImage.named(targetImageName())
        targetImage.setDockerEnvironment(emptyMap())
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            containerizer,
            forDocker = true,
            offlineMode = offlineMode.get(),
        )
    }
}
