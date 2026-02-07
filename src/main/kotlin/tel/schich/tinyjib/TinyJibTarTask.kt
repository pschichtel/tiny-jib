package tel.schich.tinyjib

import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.jib.buildTar

abstract class TinyJibTarTask(extension: TinyJibExtension) : TinyJibTask(extension) {

    @TaskAction
    fun performAction() {
        val builder = setupBuilder()
        buildTar(builder, outputDir.get().asFile.toPath())
    }

}