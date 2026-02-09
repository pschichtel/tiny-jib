package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.TarImage
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask
import javax.inject.Inject

abstract class TinyJibTarTask @Inject constructor(extension: TinyJibExtension) : TinyJibTask(extension) {

    init {
        outputs.file(extension.outputPaths.tar)
    }

    @TaskAction
    fun performAction() {
        val targetImage = TarImage.at(extension.outputPaths.tar.get().toPath())
            .named(targetImageName())
        val builder = setupBuilder()
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            builder,
            containerizer,
        )
    }
}