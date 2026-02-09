package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class PlatformParameters @Inject constructor(objectFactory: ObjectFactory) {
    @Input
    val os: Property<String> = objectFactory.property()

    @Input
    val architecture: Property<String> = objectFactory.property()
}
