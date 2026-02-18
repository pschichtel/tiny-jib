package tel.schich.tinyjib.params

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile
import java.io.File
import javax.inject.Inject

const val OUTPUT_FILE_NAME = "tiny-jib-image"

abstract class OutputPathsParameters @Inject constructor(project: Project) {
    @get:OutputFile
    abstract val digest: Property<File>

    @get:OutputFile
    abstract val tar: Property<File>

    @get:OutputFile
    abstract val imageId: Property<File>

    @get:OutputFile
    abstract val imageJson: Property<File>

    init {
        digest.convention(project.layout.buildDirectory.file("$OUTPUT_FILE_NAME.digest").map { it.asFile })
        imageId.convention(project.layout.buildDirectory.file("$OUTPUT_FILE_NAME.id").map { it.asFile })
        imageJson.convention(project.layout.buildDirectory.file("$OUTPUT_FILE_NAME.json").map { it.asFile })
        tar.convention(project.layout.buildDirectory.file("$OUTPUT_FILE_NAME.tar").map { it.asFile })
    }
}
