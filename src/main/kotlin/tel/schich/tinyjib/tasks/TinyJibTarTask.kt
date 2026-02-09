package tel.schich.tinyjib.tasks

import com.google.cloud.tools.jib.api.Containerizer
import com.google.cloud.tools.jib.api.TarImage
import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.OUTPUT_FILE_NAME
import tel.schich.tinyjib.TinyJibExtension
import tel.schich.tinyjib.TinyJibTask

abstract class TinyJibTarTask(extension: TinyJibExtension) : TinyJibTask(extension) {
    @TaskAction
    fun performAction() {
        val outputPath = outputDir.get().asFile.toPath()
        val targetImage = TarImage.at(outputPath.resolve("${OUTPUT_FILE_NAME}.tar"))
            .named(targetImageName())
        val builder = setupBuilder()
        val containerizer = Containerizer.to(targetImage)
        buildImage(
            builder,
            containerizer,
            outputPath,
            cacheDir.asFile.get().toPath(),
        )
    }
}