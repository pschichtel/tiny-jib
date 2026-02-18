package tel.schich.tinyjib.params

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

interface PlatformsBuilder {
    fun platform(block: PlatformParameters.() -> Unit)
}

private class SimplePlatformsBuilder(
    private val platforms: ListProperty<PlatformParameters>,
    private val objectFactory: ObjectFactory,
) : PlatformsBuilder {
    override fun platform(block: PlatformParameters.() -> Unit) {
        val instance = objectFactory.newInstance<PlatformParameters>()
        instance.block()
        platforms.add(instance)
    }
}

abstract class BaseImageParameters @Inject constructor(private val objectFactory: ObjectFactory) : ImageParams(objectFactory) {
    @Input
    val image: Property<String> = objectFactory.property()

    @get:Optional
    @get:Nested
    val platforms: ListProperty<PlatformParameters> = objectFactory.listProperty()

    fun platforms(block: PlatformsBuilder.() -> Unit) {
        SimplePlatformsBuilder(platforms, objectFactory).block()
    }
}
