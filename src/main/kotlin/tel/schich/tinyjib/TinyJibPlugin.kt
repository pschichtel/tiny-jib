package tel.schich.tinyjib

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import tel.schich.tinyjib.tasks.TinyJibDockerTask
import tel.schich.tinyjib.tasks.TinyJibPublishTask
import tel.schich.tinyjib.tasks.TinyJibTarTask

const val EXTENSION_NAME: String = "tinyJib"
const val BUILD_IMAGE_TASK_NAME: String = "tinyJibPublish"
const val BUILD_TAR_TASK_NAME: String = "tinyJibTag"
const val BUILD_DOCKER_TASK_NAME: String = "tinyJibDocker"


class TinyJibPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val jibExtension =
            project.extensions.create(EXTENSION_NAME, TinyJibExtension::class.java, project)

        val tasks: TaskContainer = project.tasks
        val buildImageTask =
            tasks.register(BUILD_IMAGE_TASK_NAME, TinyJibPublishTask::class.java, jibExtension)

        val buildDockerTask =
            tasks.register(BUILD_DOCKER_TASK_NAME, TinyJibDockerTask::class.java, jibExtension)

        val buildTarTask =
            tasks.register(BUILD_TAR_TASK_NAME, TinyJibTarTask::class.java, jibExtension)

    }
}
