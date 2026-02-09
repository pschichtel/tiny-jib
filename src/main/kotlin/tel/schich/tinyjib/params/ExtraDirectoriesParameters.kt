package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject

abstract class ExtraDirectoriesParameters @Inject constructor(objectFactory: ObjectFactory) {
    @get:Input
    val paths: ListProperty<ExtraDirectoryParameters> = objectFactory.listProperty()

    @get:Input
    val permissions: MapProperty<String, String> = objectFactory.mapProperty()
}
