package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.DockerDaemonImage
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask

abstract class TinyJibDockerTask(extension: TinyJibExtension) : TinyJibTask(extension) {
    @TaskAction
    fun performAction() {
        val builder = setupBuilder()
        val targetImage = DockerDaemonImage.named(targetImageName())
        targetImage.setDockerEnvironment(emptyMap())
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            builder,
            containerizer,
            outputDir.get().asFile.toPath(),
            cacheDir.asFile.get().toPath(),
        )
    }
}