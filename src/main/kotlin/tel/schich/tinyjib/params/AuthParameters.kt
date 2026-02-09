package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

class AuthParameters @Inject constructor(objectFactory: ObjectFactory) {
    @Input
    @Optional
    val username: Property<String> = objectFactory.property()
    @Input
    @Optional
    val password: Property<String> = objectFactory.property()
}
