package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

class CredHelperParameters @Inject constructor(objectFactory: ObjectFactory) {
    val environment: MapProperty<String, String> = objectFactory.mapProperty()

    @get:Optional
    @get:Input
    val helper: Property<String> = objectFactory.property()
}
