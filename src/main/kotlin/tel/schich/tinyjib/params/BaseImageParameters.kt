package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

interface PlatformsBuilder {
    fun platform(block: PlatformParameters.() -> Unit)
}

private class SimplePlatformsBuilder(
    private val platforms: ListProperty<PlatformParameters>,
    private val objectFactory: ObjectFactory,
) : PlatformsBuilder {
    override fun platform(block: PlatformParameters.() -> Unit) {
        val instance = objectFactory.newInstance(PlatformParameters::class.java)
        instance.block()
        platforms.add(instance)
    }
}

abstract class BaseImageParameters @Inject constructor(private val objectFactory: ObjectFactory) : ImageParams(objectFactory) {
    @get:Input
    abstract val image: Property<String>

    @get:Optional
    @get:Nested
    abstract val platforms: ListProperty<PlatformParameters>

    fun platforms(block: PlatformsBuilder.() -> Unit) {
        SimplePlatformsBuilder(platforms, objectFactory).block()
    }
}
