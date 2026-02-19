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
        val imageRef = targetImageName()
        val targetImage = RegistryImage.named(imageRef)
        jibService.get().configureCredentialRetrievers(imageRef, targetImage, extension.to)
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            containerizer,
            forDocker = false,
            offlineMode = false,
        )
    }
}
