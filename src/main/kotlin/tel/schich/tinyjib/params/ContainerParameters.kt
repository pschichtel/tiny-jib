package tel.schich.tinyjib.params

import com.google.cloud.tools.jib.api.JavaContainerBuilder
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class ContainerParameters @Inject constructor(objectFactory: ObjectFactory) {
    @get:Optional
    @get:Input
    val jvmFlags: ListProperty<String> = objectFactory.listProperty()

    @get:Optional
    @get:Input
    val environment: MapProperty<String, String> = objectFactory.mapProperty()

    @get:Optional
    @get:Input
    val entrypoint: ListProperty<String> = objectFactory.listProperty()

    @get:Optional
    @get:Input
    val extraClasspath: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    val expandClasspathDependencies: Property<Boolean> = objectFactory.property()

    @get:Input
    val mainClass: Property<String> = objectFactory.property()

    @get:Optional
    @get:Input
    val args: ListProperty<String> = objectFactory.listProperty()

    @get:Optional
    @get:Input
    val ports: ListProperty<String> = objectFactory.listProperty()

    @get:Optional
    @get:Input
    val volumes: ListProperty<String> = objectFactory.listProperty()

    @get:Optional
    @get:Input
    val labels: MapProperty<String, String> = objectFactory.mapProperty()

    @get:Input
    val appRoot: Property<String> = objectFactory.property()

    @get:Optional
    @get:Input
    val user: Property<String> = objectFactory.property()

    @get:Optional
    @get:Input
    val workingDirectory: Property<String> = objectFactory.property()

    @get:Input
    val filesModificationTime: Property<String> = objectFactory.property()

    @get:Input
    val creationTime: Property<String> = objectFactory.property<String>()

    init {
        expandClasspathDependencies.convention(true)
        appRoot.convention(JavaContainerBuilder.DEFAULT_APP_ROOT)
        filesModificationTime.convention("EPOCH_PLUS_SECOND")
        creationTime.convention("EPOCH")
    }
}
