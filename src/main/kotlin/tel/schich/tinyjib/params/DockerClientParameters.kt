package tel.schich.tinyjib.params

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class DockerClientParameters {
    @get:Optional
    @get:Input
    abstract val executable: Property<String>

    @get:Optional
    @get:Input
    abstract val environment: MapProperty<String, String>
}
