package tel.schich.tinyjib.params

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class PlatformParameters {
    @get:Input
    abstract val os: Property<String>

    @get:Input
    abstract val architecture: Property<String>
}
