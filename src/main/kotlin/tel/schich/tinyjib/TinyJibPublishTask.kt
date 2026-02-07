package tel.schich.tinyjib

import org.gradle.api.tasks.TaskAction
import tel.schich.tinyjib.jib.publish

abstract class TinyJibPublishTask(extension: TinyJibExtension) : TinyJibTask(extension) {

    @TaskAction
    fun performAction() {
        val builder = setupBuilder()
        publish(builder, outputDir.get().asFile.toPath())
    }

}