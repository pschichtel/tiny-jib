package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

interface PathsBuilder {
    fun path(block: ExtraDirectoryParameters.() -> Unit)
}

private class SimplePathsBuilder(
    private val paths: ListProperty<ExtraDirectoryParameters>,
    private val objectFactory: ObjectFactory,
) : PathsBuilder {
    override fun path(block: ExtraDirectoryParameters.() -> Unit) {
        val instance = objectFactory.newInstance(ExtraDirectoryParameters::class.java)
        instance.block()
        paths.add(instance)
    }
}

abstract class ExtraDirectoriesParameters @Inject constructor(private val objectFactory: ObjectFactory) {
    @get:Input
    abstract val paths: ListProperty<ExtraDirectoryParameters>

    @get:Input
    abstract val permissions: MapProperty<String, String>

    fun paths(block: PathsBuilder.() -> Unit) {
        SimplePathsBuilder(paths, objectFactory).block()
    }
}
