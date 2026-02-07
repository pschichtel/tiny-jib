package tel.schich.tinyjib

import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.jib.buildForDocker

abstract class TinyJibDockerTask(extension: TinyJibExtension) : TinyJibTask(extension) {
    @TaskAction
    fun performAction() {
        val builder = setupBuilder()
        buildForDocker(builder, outputDir.get().asFile.toPath())
    }
}