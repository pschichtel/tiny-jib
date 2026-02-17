package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.RegistryImage
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask
import javax.inject.Inject

abstract class TinyJibPublishTask @Inject constructor(extension: TinyJibExtension) : TinyJibTask(extension) {
    @TaskAction
    fun performAction() {
        val builder = setupBuilder()

        val imageRef = targetImageName()
        val targetImage = RegistryImage.named(imageRef)
        configureCredentialRetrievers(imageRef, targetImage, extension.to)
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            builder,
            containerizer,
        )
    }
}
