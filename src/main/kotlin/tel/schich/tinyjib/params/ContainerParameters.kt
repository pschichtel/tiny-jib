package tel.schich.tinyjib.params

import com.google.cloud.tools.jib.api.JavaContainerBuilder
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class ContainerParameters {
    @get:Optional
    @get:Input
    abstract val jvmFlags: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val environment: MapProperty<String, String>

    @get:Optional
    @get:Input
    abstract val entrypoint: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val extraClasspath: ListProperty<String>

    @get:Input
    abstract val mainClass: Property<String>

    @get:Optional
    @get:Input
    abstract val args: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val ports: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val volumes: ListProperty<String>

    @get:Optional
    @get:Input
    abstract val labels: MapProperty<String, String>

    @get:Input
    abstract val appRoot: Property<String>

    @get:Optional
    @get:Input
    abstract val user: Property<String>

    @get:Optional
    @get:Input
    abstract val workingDirectory: Property<String>

    @get:Input
    abstract val filesModificationTime: Property<String>

    @get:Input
    abstract val creationTime: Property<String>

    init {
        appRoot.convention(JavaContainerBuilder.DEFAULT_APP_ROOT)
        filesModificationTime.convention("EPOCH_PLUS_SECOND")
        creationTime.convention("EPOCH")
    }
}
