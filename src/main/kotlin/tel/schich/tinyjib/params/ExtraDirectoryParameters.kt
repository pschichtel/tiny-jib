package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

class ExtraDirectoryParameters @Inject constructor(objectFactory: ObjectFactory) {
    @get:Input
    val from: Property<File> = objectFactory.property()

    @get:Input
    val into: Property<String> = objectFactory.property()

    @get:Input
    @get:Optional
    val includes: ListProperty<String> = objectFactory.listProperty()

    @get:Input
    @get:Optional
    val excludes: ListProperty<String> = objectFactory.listProperty()
}
