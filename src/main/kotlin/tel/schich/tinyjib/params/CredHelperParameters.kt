package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class CredHelperParameters @Inject constructor(objectFactory: ObjectFactory) {
    @Optional
    @Input
    val environment: MapProperty<String, String> = objectFactory.mapProperty()

    @Optional
    @Input
    val helper: Property<String> = objectFactory.property()
}
