package tel.schich.tinyjib.params

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

abstract class ExtraDirectoryParameters  {
    @get:Input
    abstract val from: Property<File>

    @get:Input
    abstract val into: Property<String>

    @get:Input
    @get:Optional
    abstract val includes: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val excludes: ListProperty<String>
}
