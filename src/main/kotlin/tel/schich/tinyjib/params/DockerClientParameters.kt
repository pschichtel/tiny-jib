package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

class DockerClientParameters @Inject constructor(objectFactory: ObjectFactory) {
    @get:Optional
    @get:Input
    val executable: Property<String> = objectFactory.property()

    @get:Optional
    @get:Input
    val environment: MapProperty<String, String> = objectFactory.mapProperty()
}
