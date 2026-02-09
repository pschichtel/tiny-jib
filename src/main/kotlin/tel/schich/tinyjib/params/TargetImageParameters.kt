package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

abstract class TargetImageParameters @Inject constructor(objectFactory: ObjectFactory) : ImageParams(objectFactory) {

    @Input
    val image: Property<String> = objectFactory.property()

    @Inject
    val tags: SetProperty<String> = objectFactory.setProperty()
}
