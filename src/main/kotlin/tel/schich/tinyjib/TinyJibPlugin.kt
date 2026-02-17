package tel.schich.tinyjib

import org.gradle.api.Plugin
import org.gradle.api.Project
import tel.schich.tinyjib.service.ImageDownloadService
import tel.schich.tinyjib.tasks.TinyJibDockerTask
import tel.schich.tinyjib.tasks.TinyJibPublishTask
import tel.schich.tinyjib.tasks.TinyJibTarTask
import kotlin.reflect.KClass

const val EXTENSION_NAME: String = "tinyJib"
const val BUILD_PUBLISH_TASK_NAME: String = "tinyJibPublish"
const val BUILD_TAR_TASK_NAME: String = "tinyJibTar"
const val BUILD_DOCKER_TASK_NAME: String = "tinyJibDocker"

const val DOWNLOAD_SERVICE_NAME = "tinyJibImageDownloader"

class TinyJibPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.gradle.sharedServices.registerIfAbsent(DOWNLOAD_SERVICE_NAME, ImageDownloadService::class.java)

        val extension = project.extensions.create(
            EXTENSION_NAME,
            TinyJibExtension::class.java,
            project
        )

        fun <T : TinyJibTask> register(name: String, kclass: KClass<T>) {
            project.tasks.register(name, kclass.java, extension)
                .configure {
                    group = "container"
                }
        }


        register(BUILD_PUBLISH_TASK_NAME, TinyJibPublishTask::class)

        register(BUILD_DOCKER_TASK_NAME, TinyJibDockerTask::class)

        register(BUILD_TAR_TASK_NAME, TinyJibTarTask::class)

    }
}
