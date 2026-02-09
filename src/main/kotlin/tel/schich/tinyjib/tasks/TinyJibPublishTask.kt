package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.RegistryImage
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask

abstract class TinyJibPublishTask(extension: TinyJibExtension) : TinyJibTask(extension) {

    @TaskAction
    fun performAction() {
        val builder = setupBuilder()

        val targetImage = RegistryImage.named(targetImageName())
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            builder,
            containerizer,
        )
    }

}