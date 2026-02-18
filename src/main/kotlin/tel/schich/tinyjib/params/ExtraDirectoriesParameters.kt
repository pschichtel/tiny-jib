package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

interface PathsBuilder {
    fun path(block: ExtraDirectoryParameters.() -> Unit)
}

private class SimplePathsBuilder(
    private val paths: ListProperty<ExtraDirectoryParameters>,
    private val objectFactory: ObjectFactory,
) : PathsBuilder {
    override fun path(block: ExtraDirectoryParameters.() -> Unit) {
        val instance = objectFactory.newInstance<ExtraDirectoryParameters>()
        instance.block()
        paths.add(instance)
    }
}

abstract class ExtraDirectoriesParameters @Inject constructor(private val objectFactory: ObjectFactory) {
    @get:Input
    val paths: ListProperty<ExtraDirectoryParameters> = objectFactory.listProperty()

    @get:Input
    val permissions: MapProperty<String, String> = objectFactory.mapProperty()

    fun paths(block: PathsBuilder.() -> Unit) {
        SimplePathsBuilder(paths, objectFactory).block()
    }
}
